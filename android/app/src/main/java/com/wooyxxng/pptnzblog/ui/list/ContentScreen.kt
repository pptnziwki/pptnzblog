package com.wooyxxng.pptnzblog.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink
import com.wooyxxng.pptnzblog.ui.theme.PptnzYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()

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
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "BLOG",
                            style = MaterialTheme.typography.headlineSmall,
                            color = PptnzYellow,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBookmarksClick) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "북마크함", tint = PptnzCoral)
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "설정", tint = PptnzCoral)
                        }
                    },
                )
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text("제목·본문 검색") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
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
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().background(PptnzBackground),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            groupedByYear.forEach { (year, yearPosts) ->
                                item(key = "header_$year") {
                                    val isCollapsed = collapsedYears.contains(year)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                collapsedYears = if (isCollapsed) {
                                                    collapsedYears - year
                                                } else {
                                                    collapsedYears + year
                                                }
                                            }
                                            .padding(vertical = 10.dp),
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
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onPostClick(post) },
                                        ) {
                                            PostRow(post = post)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FloatingActionButton(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.size(44.dp),
                            containerColor = Color.White,
                            contentColor = PptnzCoral,
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
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "맨 아래로")
                        }
                    }
                }
            }
        }
    }
}
