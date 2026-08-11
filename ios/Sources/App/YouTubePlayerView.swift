import SwiftUI
import WebKit

/// 유튜브 공식 iframe 임베드 플레이어를 앱 안에서 재생하기 위한 래퍼.
/// 원문 글은 옛날 Flash `<object>/<embed>` 방식으로 영상을 삽입하지만,
/// 실제 재생은 유튜브가 공식 지원하는 `youtube-nocookie.com/embed/{id}` 페이지를 로드해서 처리한다.
///
/// 2025년 7월 유튜브 정책 변경 이후, WKWebView는 `load(URLRequest)`로 직접 열 때
/// 최상위(top-level) 내비게이션에 커스텀 `Referer` 헤더를 실제로 붙여 보내지 않는
/// iOS의 알려진 제약이 있어 오류 153/152-4로 재생이 거부된다.
/// 대신 실제 블로그 도메인을 origin으로 갖는 HTML 문서 안에 iframe으로 유튜브를
/// 심어서 로드하면, 그 iframe 내비게이션의 Referer가 해당 도메인으로 정상 전달되어
/// 실제 웹사이트에 영상이 임베드된 것과 동일한 방식으로 인식되어 재생이 허용된다.
struct YouTubePlayerView: UIViewRepresentable {
    let videoID: String

    /// 실제로 블로그 글에 영상이 임베드되는 원문 도메인. 유튜브 입장에서 정상적인
    /// 임베드 출처로 인식되도록 이 도메인을 origin으로 사용한다.
    private static let refererOrigin = URL(string: "https://peppertones.host.whoisweb.net/")!

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        let html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>html,body{margin:0;padding:0;background:transparent;}iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0;}</style>
        </head>
        <body>
        <iframe
          src="https://www.youtube-nocookie.com/embed/\(videoID)?playsinline=1&rel=0"
          referrerpolicy="strict-origin-when-cross-origin"
          allow="autoplay; encrypted-media; picture-in-picture"
          allowfullscreen></iframe>
        </body>
        </html>
        """
        webView.loadHTMLString(html, baseURL: Self.refererOrigin)
    }
}
