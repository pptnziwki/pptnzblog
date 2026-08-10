import SwiftUI
import WebKit

/// 유튜브 공식 iframe 임베드 플레이어를 앱 안에서 재생하기 위한 래퍼.
/// 원문 글은 옛날 Flash `<object>/<embed>` 방식으로 영상을 삽입하지만,
/// 실제 재생은 유튜브가 공식 지원하는 `youtube.com/embed/{id}` 페이지를 로드해서 처리한다.
struct YouTubePlayerView: UIViewRepresentable {
    let videoID: String

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard let url = URL(string: "https://www.youtube.com/embed/\(videoID)") else { return }
        webView.load(URLRequest(url: url))
    }
}
