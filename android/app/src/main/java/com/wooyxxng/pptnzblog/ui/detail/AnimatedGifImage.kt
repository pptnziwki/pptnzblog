package com.wooyxxng.pptnzblog.ui.detail

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * 애니메이션 GIF를 자동 재생하는 이미지.
 * iOS는 `CGImageSource`로 프레임을 직접 디코딩했지만, Android에서는 Coil의 GIF
 * 디코더(`coil-gif`)를 ImageLoader에 등록하기만 하면 `AsyncImage`가 자동으로
 * 애니메이션을 재생해준다.
 */
@Composable
fun AnimatedGifImage(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        imageLoader = imageLoader,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
