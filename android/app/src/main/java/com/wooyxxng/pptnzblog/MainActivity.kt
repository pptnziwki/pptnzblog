package com.wooyxxng.pptnzblog

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wooyxxng.pptnzblog.data.AppPreferences
import com.wooyxxng.pptnzblog.data.BookmarksStore
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.data.PostsRepository
import com.wooyxxng.pptnzblog.notification.DailyPostNotifier
import com.wooyxxng.pptnzblog.ui.detail.PostDetailScreen
import com.wooyxxng.pptnzblog.ui.list.BookmarksScreen
import com.wooyxxng.pptnzblog.ui.list.ContentScreen
import com.wooyxxng.pptnzblog.ui.settings.SettingsScreen
import com.wooyxxng.pptnzblog.ui.share.InstagramStorySharer
import com.wooyxxng.pptnzblog.ui.share.rememberShareCardController
import com.wooyxxng.pptnzblog.ui.theme.PptnzBlogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingPostId = mutableStateOf<String?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        pendingPostId.value = extractPostId(intent)

        setContent {
            PptnzBlogTheme {
                AppRoot(pendingPostId = pendingPostId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractPostId(intent)?.let { pendingPostId.value = it }
    }

    private fun extractPostId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return if (data.host == "post") data.lastPathSegment else null
    }
}

@Composable
private fun AppRoot(pendingPostId: MutableState<String?>) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val postsRepository = remember { PostsRepository.getInstance(context) }
    val bookmarksStore = remember { BookmarksStore.getInstance(context) }
    val appPreferences = remember { AppPreferences(context) }
    val shareCardController = rememberShareCardController()

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val bookmarkedIds by bookmarksStore.bookmarkedIdsFlow.collectAsState(initial = emptySet())
    val notificationsEnabled by appPreferences.notificationsEnabledFlow.collectAsState(initial = true)
    val dailyTimes by appPreferences.dailyTimesFlow.collectAsState(initial = emptyList())

    suspend fun refresh() {
        isLoading = true
        loadError = null
        try {
            posts = postsRepository.loadPosts()
        } catch (e: Exception) {
            loadError = e.message ?: "알 수 없는 오류"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LaunchedEffect(pendingPostId.value) {
        val id = pendingPostId.value ?: return@LaunchedEffect
        navController.navigate("detail/$id")
        pendingPostId.value = null
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            ContentScreen(
                posts = posts,
                isLoading = isLoading,
                loadError = loadError,
                onRefresh = { scope.launch { refresh() } },
                onPostClick = { post -> navController.navigate("detail/${post.id}") },
                onBookmarksClick = { navController.navigate("bookmarks") },
                onSettingsClick = { navController.navigate("settings") },
            )
        }

        composable(
            route = "detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")
            val post = posts.find { it.id == postId }

            if (post == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PostDetailScreen(
                    post = post,
                    isBookmarked = bookmarkedIds.contains(post.id),
                    onToggleBookmark = { scope.launch { bookmarksStore.toggle(post) } },
                    onShare = {
                        scope.launch {
                            val bitmap = shareCardController.capture(post)
                            InstagramStorySharer.share(context, bitmap)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable("bookmarks") {
            val bookmarkedPosts = posts.filter { bookmarkedIds.contains(it.id) }
            BookmarksScreen(
                posts = bookmarkedPosts,
                onPostClick = { post -> navController.navigate("detail/${post.id}") },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                notificationsEnabled = notificationsEnabled,
                dailyTimes = dailyTimes,
                onNotificationsEnabledChange = { enabled ->
                    scope.launch {
                        appPreferences.setNotificationsEnabled(enabled)
                        DailyPostNotifier.reschedule(context)
                    }
                },
                onDailyTimesChange = { times ->
                    scope.launch {
                        appPreferences.setDailyTimes(times)
                        DailyPostNotifier.reschedule(context)
                    }
                },
                onDismiss = { navController.popBackStack() },
            )
        }
    }
}
