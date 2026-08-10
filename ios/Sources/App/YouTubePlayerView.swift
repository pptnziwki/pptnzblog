import SwiftUI
import WebKit

/// 유튜브 공식 iframe 임베드 플레이어를 앱 안에서 재생하기 위한 래퍼.
/// 원문 글은 옛날 Flash `<object>/<embed>` 방식으로 영상을 삽입하지만,
/// 실제 재생은 유튜브가 공식 지원하는 `youtube.com/embed/{id}` 페이지를 로드해서 처리한다.
///
/// WKWebView는 `load(URLRequest)`로 직접 열든 `loadHTMLString`으로 iframe을 감싸든
/// 커스텀 스킴/로컬 콘텐츠에는 origin이 없어서 `Referer` 헤더를 자동으로 안 보낸다.
/// 유튜브 서버가 이 헤더로 임베드 출처를 검증하기 때문에, 헤더가 없으면 오류 153/152-4로
/// 재생을 거부한다. 그래서 요청에 `Referer`를 명시적으로 붙여서 로드해야 한다.
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
        guard let url = URL(string: "https://www.youtube.com/embed/\(videoID)?playsinline=1") else { return }
        var request = URLRequest(url: url)
        request.setValue("https://www.youtube.com/", forHTTPHeaderField: "Referer")
        webView.load(request)
    }
}
