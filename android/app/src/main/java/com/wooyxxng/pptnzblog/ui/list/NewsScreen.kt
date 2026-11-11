package com.wooyxxng.pptnzblog.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wooyxxng.pptnzblog.data.NewsItem
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzDivider
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink
import com.wooyxxng.pptnzblog.ui.theme.PptnzSky
import kotlinx.coroutines.launch

/** 태그 필터 칩. 주요 태그 5개와 "기타"(그 외 태그), 그리고 "전체". (iOS `NewsFilter`) */
enum class NewsFilter(val label: String, private val tag: String?) {
    ALL("전체", null),
    LIVE("LIVE", "live"),
    RELEASE("RELEASE", "release"),
    TV("TV", "tv"),
    RADIO("RADIO", "radio"),
    WORK("WORK", "work"),
    OTHER("기타", null);

    fun matches(item: NewsItem): Boolean = when (this) {
        ALL -> true
        OTHER -> item.tags.none { it in PRIMARY_TAGS }
        else -> tag in item.tags
    }

    companion object {
        private val PRIMARY_TAGS = setOf("live", "release", "tv", "radio", "work")
    }
}

/**
 * peppertones.net news 탭 내용을 연도별 토글 목록으로 보여준다. (iOS `NewsView`)
 * 연도는 기본적으로 모두 펼쳐져 있다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewsScreen(
    items: List<NewsItem>,
    isLoading: Boolean,
    loadError: String?,
    attendedIds: Set<String>,
    onToggleAttended: (NewsItem) -> Unit,
    onRefresh: () -> Unit,
    onAttendedClick: () -> Unit,
    onSettingsClick: () -> Unit,
    section: HomeSection,
    onSectionChange: (HomeSection) -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(NewsFilter.ALL.name) }
    var collapsedYears by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val filter = NewsFilter.valueOf(filterName)
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val noRipple = remember { MutableInteractionSource() }
    val uriHandler = LocalUriHandler.current

    val isSearching = searchText.isNotBlank()
    // 검색이나 태그 필터가 걸려 있으면 접힌 연도에 있는 결과도 보이도록 항상 펼친다.
    val forcesExpansion = isSearching || filter != NewsFilter.ALL

    val filteredItems = remember(items, searchText, filter) {
        items.filter { filter.matches(it) }.filter {
            !isSearching ||
                it.content.contains(searchText, ignoreCase = true) ||
                it.tag.contains(searchText, ignoreCase = true) ||
                it.date.contains(searchText, ignoreCase = true)
        }
    }

    // 연도 내림차순, 각 연도 안에서는 날짜 최신순(날짜가 같으면 원문 순서 유지: sortedByDescending은 안정 정렬).
    val groupedByYear = remember(filteredItems) {
        filteredItems.groupBy { it.year }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value.sortedByDescending { item -> item.sortKey } }
    }

    Scaffold(
        containerColor = PptnzBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { SectionToggle(selection = section, onSelectionChange = onSectionChange) },
                navigationIcon = {
                    // NEWS에서는 북마크 대신 "다녀온 공연" 모아보기
                    IconButton(onClick = onAttendedClick) {
                        Icon(
                            Icons.Outlined.ConfirmationNumber,
                            contentDescription = "다녀온 공연",
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
        bottomBar = { SearchBottomBar(searchText = searchText, onSearchTextChange = { searchText = it }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (items.isNotEmpty()) {
                FilterBar(selected = filter, onSelect = { filterName = it.name })
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    items.isEmpty() && isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    loadError != null && items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("소식을 불러오지 못했어요\n$loadError", textAlign = TextAlign.Center)
                        }
                    }
                    filteredItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                when {
                                    isSearching -> "검색 결과가 없어요"
                                    filter != NewsFilter.ALL -> "${filter.label} 소식이 없어요"
                                    else -> "아직 소식이 없어요"
                                },
                            )
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
                                groupedByYear.forEach { (year, yearItems) ->
                                    stickyHeader(key = "news_header_$year") {
                                        val isExpanded = forcesExpansion || year !in collapsedYears
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(PptnzBackground)
                                                .clickable(interactionSource = noRipple, indication = null) {
                                                    collapsedYears = if (year in collapsedYears) {
                                                        collapsedYears - year
                                                    } else {
                                                        collapsedYears + year
                                                    }
                                                }
                                                .padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("${year}년", color = PptnzPink, style = MaterialTheme.typography.titleMedium)
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = PptnzPink,
                                            )
                                        }
                                    }
                                    if (forcesExpansion || year !in collapsedYears) {
                                        items(yearItems, key = { it.id }) { item ->
                                            Column {
                                                NewsRow(
                                                    item = item,
                                                    isAttended = item.id in attendedIds,
                                                    onToggleAttended = { onToggleAttended(item) },
                                                    onOpenLink = { link -> runCatching { uriHandler.openUri(link) } },
                                                )
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
                            ScrollFab(Icons.Filled.ArrowUpward, "맨 위로") {
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                            ScrollFab(Icons.Filled.ArrowDownward, "맨 아래로") {
                                scope.launch {
                                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                                    if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollFab(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        containerColor = Color.White,
        contentColor = PptnzCoral,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 2.dp),
    ) {
        Icon(icon, contentDescription = description)
    }
}

/** 상단 태그 필터. 한 번에 하나만 선택한다. 좁은 화면에서는 가로로 스크롤된다. */
@Composable
private fun FilterBar(selected: NewsFilter, onSelect: (NewsFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PptnzBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NewsFilter.entries.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = CircleShape,
                color = if (isSelected) PptnzSky else PptnzInk.copy(alpha = 0.08f),
            ) {
                Text(
                    option.label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) PptnzInk else PptnzInk.copy(alpha = 0.55f),
                )
            }
        }
    }
}

/** 소식 한 줄. 링크가 있으면 눌러서 열고, live 소식에는 다녀온 공연 체크박스가 붙는다. */
@Composable
fun NewsRow(
    item: NewsItem,
    isAttended: Boolean,
    onToggleAttended: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    enabled = item.link != null,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { item.link?.let(onOpenLink) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = PptnzInk.copy(alpha = 0.08f)) {
                    Text(
                        item.tag.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PptnzInk.copy(alpha = 0.55f),
                    )
                }
                Text(item.date, fontSize = 12.sp, color = PptnzInk.copy(alpha = 0.55f))
                if (item.link != null) {
                    Icon(
                        Icons.Filled.Link,
                        contentDescription = "링크",
                        tint = PptnzInk.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(item.content, style = MaterialTheme.typography.bodyMedium, color = PptnzInk)
        }
        if (item.isLive) {
            Checkbox(
                checked = isAttended,
                onCheckedChange = { onToggleAttended() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PptnzCoral,
                    uncheckedColor = PptnzInk.copy(alpha = 0.45f),
                ),
            )
        }
    }
}
