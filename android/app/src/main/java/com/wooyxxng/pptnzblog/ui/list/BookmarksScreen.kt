package com.wooyxxng.pptnzblog.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzDivider
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    posts: List<Post>,
    onPostClick: (Post) -> Unit,
    onDismiss: () -> Unit,
) {
    Scaffold(
        containerColor = PptnzBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("북마크함", fontWeight = FontWeight.Bold, color = PptnzInk) },
                actions = { TextButton(onClick = onDismiss) { Text("닫기") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(PptnzBackground)) {
            if (posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("저장한 글이 없어요")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    items(posts, key = { it.id }) { post ->
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { onPostClick(post) },
                            ) {
                                PostRow(post = post, modifier = Modifier.fillMaxWidth())
                            }
                            HorizontalDivider(color = PptnzDivider, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
