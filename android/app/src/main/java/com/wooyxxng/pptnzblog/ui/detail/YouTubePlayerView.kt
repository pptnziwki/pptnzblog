package com.wooyxxng.pptnzblog.ui.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
 *
 * 또한 유튜브 iframe 플레이어는 `playsinline`이어도 내부적으로 HTML5 비디오의
 * 전체화면 API(`<video>`의 custom view)를 통해 프레임을 그리는 경우가 있는데,
 * 기본 `WebChromeClient`가 없으면 이 custom view를 붙일 곳이 없어 오디오만 재생되고
 * 화면은 흰 배경만 보이는 문제가 있었다. `onShowCustomView`/`onHideCustomView`를
 * 구현해 WebView를 감싸는 컨테이너에 custom view를 붙여준다.
 */
private const val REFERER_ORIGIN = "https://peppertones.host.whoisweb.net/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val container = FrameLayout(context)
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                setBackgroundColor(Color.parseColor("#1F1F1F"))
                webViewClient = WebViewClient()
            }
            webView.webChromeClient = object : WebChromeClient() {
                private var customView: View? = null
                private var customViewCallback: CustomViewCallback? = null

                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (customView != null) {
                        callback.onCustomViewHidden()
                        return
                    }
                    customView = view
                    customViewCallback = callback
                    webView.visibility = View.GONE
                    container.addView(
                        view,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }

                override fun onHideCustomView() {
                    val view = customView ?: return
                    container.removeView(view)
                    customView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                    webView.visibility = View.VISIBLE
                }
            }
            container.addView(
                webView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            container
        },
        update = { container ->
            val webView = container.getChildAt(0) as WebView
            val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0"
            webView.loadUrl(embedUrl, mapOf("Referer" to REFERER_ORIGIN))
        },
    )
}
