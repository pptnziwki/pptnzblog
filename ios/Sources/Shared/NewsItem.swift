import Foundation

/// peppertones.net news 탭의 소식 한 건.
/// crawl_pptnz_news.py가 생성하는 news.json의 각 항목과 1:1로 대응한다.
struct NewsItem: Identifiable, Codable, Hashable {
    /// 내용 기반 해시. 새 소식 감지에 쓰이므로 순서와 무관하게 안정적이다.
    let id: String
    /// "live", "tv", "release" 등 원문의 `-tag`. 소문자로 정규화되어 있다.
    let tag: String
    /// 원문 날짜 표기 그대로. ("2026/11/1", "2026/12/11-13" 등)
    let date: String
    let year: Int
    let content: String
    /// 소식에 달려 있는 첫 번째 링크(예매처 등). 없으면 nil.
    let link: String?

    var yearLabel: String { "\(year)년" }

    /// 같은 연도 안에서 최신순으로 정렬하기 위한 값 (yyyymmdd).
    /// 원문은 "2026/12/11-13", "2024/9-" 처럼 일/월이 빠지거나 범위인 경우가 있어서
    /// 날짜 문자열 안의 첫 `연/월(/일)`만 읽고, 없는 부분은 0으로 둔다.
    var sortKey: Int {
        guard let match = date.range(of: #"(\d{4})/(\d{1,2})(?:/(\d{1,2}))?"#, options: .regularExpression) else {
            return year * 10000
        }
        let parts = date[match].split(separator: "/").compactMap { Int($0) }
        let month = parts.count > 1 ? parts[1] : 0
        let day = parts.count > 2 ? parts[2] : 0
        return year * 10000 + month * 100 + day
    }

    var linkURL: URL? {
        link.flatMap { URL(string: $0) }
    }
}
