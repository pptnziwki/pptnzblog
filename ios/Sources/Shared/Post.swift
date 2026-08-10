import Foundation

/// PPTNZ 블로그 글 한 편.
/// crawl_pptnz_blog.py가 생성하는 posts.json의 각 항목과 1:1로 대응한다.
struct Post: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let link: String
    let date: String
    let content: String

    private enum CodingKeys: String, CodingKey {
        case id, title, link, date, content
    }

    /// 제목이 없는 글은 항상 "-"로 표시하기 위해, 생성/디코딩 시점에 한 번만 정규화한다.
    init(id: String, title: String, link: String, date: String, content: String) {
        self.id = id
        self.title = title.isEmpty ? "-" : title
        self.link = link
        self.date = date
        self.content = content
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let rawTitle = try container.decode(String.self, forKey: .title)
        self.init(
            id: try container.decode(String.self, forKey: .id),
            title: rawTitle,
            link: try container.decode(String.self, forKey: .link),
            date: try container.decode(String.self, forKey: .date),
            content: try container.decode(String.self, forKey: .content)
        )
    }

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

    /// 새 글 감지 시 문자열이 아닌 숫자로 비교하기 위한 편의 프로퍼티.
    var numericID: Int {
        Int(id) ?? 0
    }

    /// SwiftUI #Preview용 샘플 데이터.
    static let preview = Post(
        id: "1",
        title: "샘플 제목",
        link: "https://example.com",
        date: "2024.03.15",
        content: "본문 미리보기 텍스트입니다."
    )
}
