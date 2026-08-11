package com.wooyxxng.pptnzblog.ui.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 유튜브 공식 iframe 임베드 플레이어를 앱 안에서 재생하기 위한 래퍼.
 *
 * iOS `YouTubePlayerView.swift`와 동일한 해결책을 재사용한다: WebView가 최상위
 * 내비게이션에 커스텀 Referer를 붙여 보내지 못하는 제약을 피하기 위해, 실제 블로그
 * 도메인을 origin으로 갖는 HTML 문서 안에 iframe으로 유튜브(`youtube-nocookie.com`)를
 * 심어서 로드한다. 그러면 iframe 내비게이션의 Referer가 해당 도메인으로 정상
 * 전달되어, 실제 웹사이트에 영상이 임베드된 것과 동일하게 인식되어 재생이 허용된다.
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
                setBackgroundColor(Color.TRANSPARENT)
            }
        },
        update = { webView ->
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                <style>html,body{margin:0;padding:0;background:transparent;}iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0;}</style>
                </head>
                <body>
                <iframe
                  src="https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0"
                  referrerpolicy="strict-origin-when-cross-origin"
                  allow="autoplay; encrypted-media; picture-in-picture"
                  allowfullscreen></iframe>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(REFERER_ORIGIN, html, "text/html", "utf-8", null)
        },
    )
}
