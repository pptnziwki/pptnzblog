import Foundation

/// GitHub Pages(docs/)에 올라가는 글별 공유 페이지(generate_share_pages.py가 생성) 관련 상수.
/// 공유 페이지는 카카오톡/메시지 등에서 열어볼 미리보기(OG 태그)를 담당하고,
/// 앱이 설치돼 있으면 pptnzblog://post/{id} 스킴으로, 없으면 원문으로 리다이렉트한다.
enum SharePagesConfig {
    static let baseURL = "https://pptnziwki.github.io/pptnzblog"

    static func url(for postID: String) -> URL? {
        URL(string: "\(baseURL)/s/\(postID).html")
    }
}
