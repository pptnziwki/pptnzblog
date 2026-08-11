package com.wooyxxng.pptnzblog.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.data.PostComment
import com.wooyxxng.pptnzblog.data.PostContentBlock
import com.wooyxxng.pptnzblog.data.PostDetail
import com.wooyxxng.pptnzblog.parsing.PostDetailLoader
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink
import com.wooyxxng.pptnzblog.ui.theme.PptnzTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: Post,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    var detail by remember(post.id) { mutableStateOf<PostDetail?>(null) }
    var isLoading by remember(post.id) { mutableStateOf(true) }
    var loadFailed by remember(post.id) { mutableStateOf(false) }
    var showsAllComments by remember(post.id) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(post.id) {
        isLoading = true
        loadFailed = false
        try {
            detail = PostDetailLoader.shared.load(post)
        } catch (e: Exception) {
            loadFailed = true
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = PptnzBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = PptnzCoral)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "북마크",
                            tint = PptnzCoral,
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "공유", tint = PptnzCoral)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PptnzBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(post.title, style = MaterialTheme.typography.headlineSmall, color = PptnzPink)
            Text(post.date, style = MaterialTheme.typography.bodySmall, color = PptnzTeal)

            BodyContent(detail = detail, post = post, isLoading = isLoading, loadFailed = loadFailed)

            Text(
                "블로그에서 원문 보기",
                color = PptnzCoral,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { uriHandler.openUri(post.link) },
            )

            detail?.let { d ->
                if (d.links.isNotEmpty()) {
                    LinksSection(links = d.links, uriHandler = { uriHandler.openUri(it) })
                }
                CommentsSection(
                    comments = d.comments,
                    showsAll = showsAllComments,
                    onToggleShowsAll = { showsAllComments = !showsAllComments },
                )
            }
        }
    }
}

@Composable
private fun BodyContent(detail: PostDetail?, post: Post, isLoading: Boolean, loadFailed: Boolean) {
    when {
        detail != null && detail.blocks.isNotEmpty() -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                detail.blocks.forEach { block ->
                    when (block) {
                        is PostContentBlock.Text -> Text(block.text, color = PptnzInk)
                        is PostContentBlock.Image -> {
                            if (block.url.lowercase().endsWith(".gif")) {
                                AnimatedGifImage(
                                    url = block.url,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            } else {
                                AsyncImage(
                                    model = block.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                        is PostContentBlock.YouTube -> {
                            val uriHandler = LocalUriHandler.current
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                YouTubePlayerView(
                                    videoId = block.videoId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Text(
                                    "유튜브에서 보기",
                                    color = PptnzCoral,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.clickable {
                                        uriHandler.openUri("https://www.youtube.com/watch?v=${block.videoId}")
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        isLoading -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("원문을 불러오는 중…", modifier = Modifier.padding(top = 8.dp))
            }
        }
        else -> {
            // 실시간 파싱에 실패해도 posts.json에 저장된 텍스트는 그대로 보여준다.
            Text(post.content, color = PptnzInk)
            if (loadFailed) {
                Text(
                    "원문 페이지를 불러오지 못해 요약 내용만 표시하고 있어요.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun LinksSection(links: List<String>, uriHandler: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("첨부 링크", style = MaterialTheme.typography.titleMedium, color = PptnzPink)
        links.forEach { url ->
            Text(
                url,
                color = PptnzCoral,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.clickable { uriHandler(url) },
            )
        }
    }
}

@Composable
private fun CommentsSection(comments: List<PostComment>, showsAll: Boolean, onToggleShowsAll: () -> Unit) {
    val visibleComments = if (showsAll) comments else comments.take(3)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("댓글 ${comments.size}개", style = MaterialTheme.typography.titleMedium, color = PptnzPink)

        if (comments.isEmpty()) {
            Text("아직 댓글이 없어요.", style = MaterialTheme.typography.bodySmall)
        } else {
            visibleComments.forEach { comment ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(comment.author, style = MaterialTheme.typography.labelMedium, color = PptnzTeal)
                        if (comment.date.isNotEmpty()) {
                            Text(comment.date, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(comment.text, color = PptnzInk)
                }
            }

            if (comments.size > 3) {
                Text(
                    if (showsAll) "댓글 접기" else "댓글 ${comments.size - 3}개 더보기",
                    color = PptnzCoral,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { onToggleShowsAll() },
                )
            }
        }
    }
}
