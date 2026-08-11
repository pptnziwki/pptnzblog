package com.wooyxxng.pptnzblog.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzDivider
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink
import com.wooyxxng.pptnzblog.ui.theme.PptnzYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContentScreen(
    posts: List<Post>,
    isLoading: Boolean,
    loadError: String?,
    onRefresh: () -> Unit,
    onPostClick: (Post) -> Unit,
    onBookmarksClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var collapsedYears by rememberSaveable { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val noRipple = remember { MutableInteractionSource() }

    val filteredPosts = remember(posts, searchText) {
        if (searchText.isBlank()) {
            posts
        } else {
            posts.filter {
                it.title.contains(searchText, ignoreCase = true) ||
                    it.content.contains(searchText, ignoreCase = true)
            }
        }
    }

    val groupedByYear = remember(filteredPosts) {
        filteredPosts.groupBy { it.yearLabel }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value.sortedByDescending { post -> post.id } }
    }

    Scaffold(
        containerColor = PptnzBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "BLOG",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = PptnzYellow,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBookmarksClick) {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = "북마크함",
                            tint = PptnzCoral,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "설정",
                            tint = PptnzCoral,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = PptnzBackground, tonalElevation = 0.dp, modifier = Modifier.imePadding()) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
                    placeholder = {
                        Text(
                            "제목·본문 검색",
                            fontSize = 13.sp,
                            color = PptnzInk.copy(alpha = 0.35f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = PptnzDivider,
                        focusedBorderColor = PptnzDivider,
                    ),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                posts.isEmpty() && isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                loadError != null && posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("글을 불러오지 못했어요\n$loadError", textAlign = TextAlign.Center)
                    }
                }
                filteredPosts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없어요")
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = onRefresh,
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullToRefreshState,
                                isRefreshing = isLoading,
                                containerColor = PptnzBackground,
                                color = PptnzCoral,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        },
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().background(PptnzBackground),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            groupedByYear.forEach { (year, yearPosts) ->
                                stickyHeader(key = "header_$year") {
                                    val isCollapsed = collapsedYears.contains(year)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(PptnzBackground)
                                            .clickable(
                                                interactionSource = noRipple,
                                                indication = null,
                                            ) {
                                                collapsedYears = if (isCollapsed) {
                                                    collapsedYears - year
                                                } else {
                                                    collapsedYears + year
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(year, color = PptnzPink, style = MaterialTheme.typography.titleMedium)
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = PptnzPink,
                                        )
                                    }
                                }
                                if (!collapsedYears.contains(year)) {
                                    items(yearPosts, key = { it.id }) { post ->
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                    ) { onPostClick(post) },
                                            ) {
                                                PostRow(post = post)
                                            }
                                            HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FloatingActionButton(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.size(44.dp),
                            containerColor = Color.White,
                            contentColor = PptnzCoral,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 2.dp,
                            ),
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "맨 위로")
                        }
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                                    if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            containerColor = Color.White,
                            contentColor = PptnzCoral,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 2.dp,
                            ),
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "맨 아래로")
                        }
                    }
                }
            }
        }
    }
}
