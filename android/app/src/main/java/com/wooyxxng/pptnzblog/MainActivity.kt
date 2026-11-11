package com.wooyxxng.pptnzblog

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.wooyxxng.pptnzblog.data.AttendedStore
import com.wooyxxng.pptnzblog.data.NewsItem
import com.wooyxxng.pptnzblog.data.NewsRepository
import com.wooyxxng.pptnzblog.notification.NewsNotifier
import com.wooyxxng.pptnzblog.ui.list.AttendedScreen
import com.wooyxxng.pptnzblog.ui.list.HomeSection
import com.wooyxxng.pptnzblog.ui.list.NewsScreen
import androidx.compose.runtime.saveable.rememberSaveable
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
    /** 새 소식 알림을 탭해서 열린 경우 NEWS 탭으로 전환하기 위한 플래그. */
    private val pendingNews = mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        pendingPostId.value = extractPostId(intent)
        pendingNews.value = isNewsIntent(intent)

        setContent {
            PptnzBlogTheme {
                AppRoot(pendingPostId = pendingPostId, pendingNews = pendingNews)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractPostId(intent)?.let { pendingPostId.value = it }
        if (isNewsIntent(intent)) pendingNews.value = true
    }

    private fun isNewsIntent(intent: Intent?): Boolean = intent?.data?.host == "news"

    private fun extractPostId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return if (data.host == "post") data.lastPathSegment else null
    }
}

@Composable
private fun AppRoot(pendingPostId: MutableState<String?>, pendingNews: MutableState<Boolean>) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val postsRepository = remember { PostsRepository.getInstance(context) }
    val bookmarksStore = remember { BookmarksStore.getInstance(context) }
    val appPreferences = remember { AppPreferences(context) }
    val newsRepository = remember { NewsRepository.getInstance(context) }
    val attendedStore = remember { AttendedStore.getInstance(context) }
    val shareCardController = rememberShareCardController()

    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var section by rememberSaveable { mutableStateOf(HomeSection.BLOG.name) }
    var newsItems by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isNewsLoading by remember { mutableStateOf(true) }
    var newsError by remember { mutableStateOf<String?>(null) }

    val bookmarkedIds by bookmarksStore.bookmarkedIdsFlow.collectAsState(initial = emptySet())
    val attendedItems by attendedStore.itemsFlow.collectAsState(initial = emptyList())
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

    suspend fun refreshNews() {
        isNewsLoading = true
        newsError = null
        try {
            newsItems = newsRepository.loadNews()
            // NEWS 탭에서 본 소식은 이후 백그라운드 새로고침이 "새 소식"으로 알리지 않게 기록한다.
            NewsNotifier.markSeen(context, newsItems)
        } catch (e: Exception) {
            newsError = e.message ?: "알 수 없는 오류"
        } finally {
            isNewsLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(Unit) { refreshNews() }

    LaunchedEffect(pendingNews.value) {
        if (!pendingNews.value) return@LaunchedEffect
        navController.popBackStack("list", inclusive = false)
        section = HomeSection.NEWS.name
        pendingNews.value = false
    }

    LaunchedEffect(pendingPostId.value) {
        val id = pendingPostId.value ?: return@LaunchedEffect
        navController.navigate("detail/$id")
        pendingPostId.value = null
    }

    NavHost(
        navController = navController,
        startDestination = "list",
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(150)) },
        popExitTransition = { fadeOut(tween(150)) },
    ) {
        composable("list") {
            if (section == HomeSection.NEWS.name) {
                NewsScreen(
                    items = newsItems,
                    isLoading = isNewsLoading,
                    loadError = newsError,
                    attendedIds = attendedItems.map { it.id }.toSet(),
                    onToggleAttended = { item -> scope.launch { attendedStore.toggle(item) } },
                    onRefresh = { scope.launch { refreshNews() } },
                    onAttendedClick = { navController.navigate("attended") },
                    onSettingsClick = { navController.navigate("settings") },
                    section = HomeSection.NEWS,
                    onSectionChange = { section = it.name },
                )
            } else {
                ContentScreen(
                    posts = posts,
                    isLoading = isLoading,
                    loadError = loadError,
                    onRefresh = { scope.launch { refresh() } },
                    onPostClick = { post -> navController.navigate("detail/${post.id}") },
                    onBookmarksClick = { navController.navigate("bookmarks") },
                    onSettingsClick = { navController.navigate("settings") },
                    section = HomeSection.BLOG,
                    onSectionChange = { section = it.name },
                )
            }
        }

        composable("attended") {
            AttendedScreen(
                items = attendedItems,
                onToggleAttended = { item -> scope.launch { attendedStore.toggle(item) } },
                onDismiss = { navController.popBackStack() },
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
