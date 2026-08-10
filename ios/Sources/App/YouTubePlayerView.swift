import SwiftUI
import WebKit

/// 유튜브 공식 iframe 임베드 플레이어를 앱 안에서 재생하기 위한 래퍼.
/// 원문 글은 옛날 Flash `<object>/<embed>` 방식으로 영상을 삽입하지만,
/// 실제 재생은 유튜브가 공식 지원하는 `youtube.com/embed/{id}` 페이지를 로드해서 처리한다.
///
/// `webView.load(URLRequest(url:))`로 embed URL을 직접 열면 WKWebView가 올바른
/// Referer/Origin 헤더를 보내지 않아 유튜브 쪽에서 "오류 153(Video player configuration
/// error)"을 내며 재생을 거부한다. iframe을 담은 HTML을 `referrerpolicy`와 함께
/// youtube.com을 baseURL로 직접 로드해 이를 우회한다.
struct YouTubePlayerView: UIViewRepresentable {
    let videoID: String

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        let html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>html, body { margin: 0; padding: 0; background: #000; }
            iframe { width: 100%; height: 100%; border: 0; }</style>
        </head>
        <body>
            <iframe
                src="https://www.youtube.com/embed/\(videoID)?playsinline=1"
                referrerpolicy="strict-origin-when-cross-origin"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """
        webView.loadHTMLString(html, baseURL: URL(string: "https://www.youtube.com"))
    }
}
