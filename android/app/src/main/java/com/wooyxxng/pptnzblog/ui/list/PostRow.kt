package com.wooyxxng.pptnzblog.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzTeal

@Composable
fun PostRow(post: Post, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            color = PptnzInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = post.date,
            style = MaterialTheme.typography.labelSmall,
            color = PptnzTeal,
        )
        if (post.content.isNotEmpty()) {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
