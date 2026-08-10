import Foundation

/// PPTNZ 블로그 글 한 편.
/// crawl_pptnz_blog.py가 생성하는 posts.json의 각 항목과 1:1로 대응한다.
struct Post: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let link: String
    let date: String
    let content: String

    /// date 문자열에서 연도(4자리 숫자)를 뽑아낸다.
    /// Textcube 날짜 표기 형식이 정확히 어떤지 확정되지 않아서,
    /// 문자열 안 첫 4자리 숫자를 연도로 간주하는 방어적인 방식으로 파싱한다.
    /// (실제 posts.json을 받아본 뒤 형식이 다르면 이 정규식만 손보면 된다.)
    var year: Int? {
        guard let match = date.range(of: #"\d{4}"#, options: .regularExpression) else {
            return nil
        }
        return Int(date[match])
    }

    var yearLabel: String {
        year.map { "\($0)년" } ?? "연도 미상"
    }
}
