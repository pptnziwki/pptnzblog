package com.wooyxxng.pptnzblog.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wooyxxng.pptnzblog.R
import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.ui.theme.PptnzBackground
import com.wooyxxng.pptnzblog.ui.theme.PptnzCoral
import com.wooyxxng.pptnzblog.ui.theme.PptnzInk
import com.wooyxxng.pptnzblog.ui.theme.PptnzPink

object PostShareCardSpec {
    val cardWidth = 320.dp
    val cardHeight = 427.dp
    val cornerRadius = 28.dp
    val shadowPadding = 24.dp
    val canvasWidth = cardWidth + shadowPadding * 2
    val canvasHeight = cardHeight + shadowPadding * 2
}

@Composable
fun PostShareCard(post: Post) {
    Box(
        modifier = Modifier
            .size(PostShareCardSpec.canvasWidth, PostShareCardSpec.canvasHeight)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .size(PostShareCardSpec.cardWidth, PostShareCardSpec.cardHeight)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(PostShareCardSpec.cornerRadius),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(PostShareCardSpec.cornerRadius))
                .background(PptnzBackground)
                .padding(24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(26.dp),
            )

            Text(
                text = post.title,
                modifier = Modifier.padding(top = 12.dp),
                color = PptnzPink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = post.content,
                color = PptnzInk,
                fontSize = 14.sp,
                maxLines = 14,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = post.date,
                    color = PptnzInk.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
                Text(
                    text = "pptnz.net",
                    color = PptnzCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
