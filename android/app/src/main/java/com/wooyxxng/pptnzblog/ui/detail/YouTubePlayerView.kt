package com.wooyxxng.pptnzblog.ui.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 유튜브 공식 iframe 임베드 플레이어를 앱 안에서 재생하기 위한 래퍼.
 *
 * `loadDataWithBaseURL`로 커스텀 HTML 문서를 만들어 그 안에 iframe으로 유튜브를 심는
 * 방식은 Android WebView에서 baseUrl이 iframe 하위 리소스 내비게이션의 Referer로
 * 안정적으로 전달되지 않아, 도메인 제한이 걸린 임베드가 재생되지 않는 문제가 있었다.
 * 대신 유튜브 임베드 URL 자체를 최상위 내비게이션으로 직접 로드하면서 `Referer` HTTP
 * 헤더를 명시적으로 지정하는 방식을 사용해 신뢰성을 높인다.
 */
private const val REFERER_ORIGIN = "https://peppertones.host.whoisweb.net/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                setBackgroundColor(Color.parseColor("#1F1F1F"))
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0"
            webView.loadUrl(embedUrl, mapOf("Referer" to REFERER_ORIGIN))
        },
    )
}
