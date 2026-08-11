package com.wooyxxng.pptnzblog.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.withFrameNanos
import androidx.compose.ui.unit.dp
import com.wooyxxng.pptnzblog.data.Post
import kotlinx.coroutines.CompletableDeferred

/**
 * PostShareCard를 화면 밖에서 렌더링해 Bitmap으로 캡처한다.
 * iOS의 `PostShareCardRenderer`(ImageRenderer 기반)에 대응.
 */
class ShareCardController internal constructor(
    private val requestCapture: (Post, CompletableDeferred<Bitmap>) -> Unit,
) {
    suspend fun capture(post: Post): Bitmap {
        val deferred = CompletableDeferred<Bitmap>()
        requestCapture(post, deferred)
        return deferred.await()
    }
}

@Composable
fun rememberShareCardController(): ShareCardController {
    var pendingRequest by remember { mutableStateOf<Pair<Post, CompletableDeferred<Bitmap>>?>(null) }
    val graphicsLayer = rememberGraphicsLayer()

    val controller = remember {
        ShareCardController { post, deferred ->
            pendingRequest = post to deferred
        }
    }

    val request = pendingRequest
    if (request != null) {
        val (post, deferred) = request
        Box(
            modifier = Modifier
                .offset(x = 4000.dp)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                },
        ) {
            PostShareCard(post = post)
        }

        LaunchedEffect(post, deferred) {
            // 레이아웃/드로우가 반영될 시간을 확보하기 위해 두 프레임 대기한다.
            withFrameNanos { }
            withFrameNanos { }
            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
            deferred.complete(bitmap)
            pendingRequest = null
        }
    }

    return controller
}
