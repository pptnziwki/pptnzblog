package com.wooyxxng.pptnzblog.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val cardMinHeight = 427.dp
    val cornerRadius = 28.dp
    val shadowPadding = 24.dp
    val canvasWidth = cardWidth + shadowPadding * 2
}

/**
 * `Modifier.shadow()`(elevation 기반 네이티브 그림자)는 화면에는 보이지만,
 * `ShareCardRenderer`가 쓰는 `GraphicsLayer.record { }.toImageBitmap()` 오프스크린
 * 캡처 경로에는 전혀 포함되지 않아 내보낸 이미지에 그림자가 아예 안 찍히는 문제가 있었다.
 * elevation 대신 반투명 라운드 사각형을 여러 겹 겹쳐 그리는 방식으로 블러를 흉내 내면
 * 일반 벡터 드로우 명령이라 캡처 경로에서도 그대로 렌더링된다.
 */
private fun Modifier.softCardShadow(
    cornerRadius: Dp,
    color: Color = Color.Black,
    alpha: Float = 0.06f,
    blurRadius: Dp = 20.dp,
    offsetY: Dp = 6.dp,
    layers: Int = 12,
): Modifier = drawBehind {
    val cornerPx = cornerRadius.toPx()
    val blurPx = blurRadius.toPx()
    val offsetYPx = offsetY.toPx()
    val stepAlpha = alpha / layers

    for (i in layers downTo 1) {
        val spread = blurPx * i / layers
        drawRoundRect(
            color = color.copy(alpha = stepAlpha),
            topLeft = Offset(-spread, offsetYPx - spread),
            size = Size(size.width + spread * 2, size.height + spread * 2),
            cornerRadius = CornerRadius(cornerPx + spread, cornerPx + spread),
        )
    }
}

@Composable
fun PostShareCard(post: Post) {
    Box(
        modifier = Modifier
            .width(PostShareCardSpec.canvasWidth)
            .background(Color.Transparent)
            .padding(PostShareCardSpec.shadowPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(PostShareCardSpec.cardWidth)
                .defaultMinSize(minHeight = PostShareCardSpec.cardMinHeight)
                .softCardShadow(cornerRadius = PostShareCardSpec.cornerRadius)
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

            Spacer(modifier = Modifier.height(20.dp))

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
