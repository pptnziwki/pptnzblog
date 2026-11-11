import Foundation

/// 사용자가 "다녀온 공연"으로 체크한 소식을 저장한다. (블로그 북마크와 같은 역할)
///
/// news.json의 id는 내용 해시라서, 원문이 수정되면 id가 바뀔 수 있다.
/// 그래서 id만이 아니라 소식 내용 전체를 저장해두고, 모아보기는 저장된 내용으로 보여준다.
/// (사이트에서 문구가 바뀌거나 항목이 사라져도 체크한 공연은 남는다.)
final class AttendedStore: ObservableObject {
    static let shared = AttendedStore()

    @Published private(set) var items: [NewsItem]

    private let defaultsKey = "attendedNewsItems"
    private let defaults = UserDefaults(suiteName: PostsRepository.appGroupID)

    private init() {
        if let data = defaults?.data(forKey: defaultsKey),
           let decoded = try? JSONDecoder().decode([NewsItem].self, from: data) {
            items = decoded
        } else {
            items = []
        }
    }

    func isAttended(_ item: NewsItem) -> Bool {
        items.contains { $0.id == item.id }
    }

    func toggle(_ item: NewsItem) {
        if let index = items.firstIndex(where: { $0.id == item.id }) {
            items.remove(at: index)
        } else {
            items.append(item)
        }
        if let data = try? JSONEncoder().encode(items) {
            defaults?.set(data, forKey: defaultsKey)
        }
    }

    /// 최신 공연이 위로 오도록 정렬한 목록.
    var sortedItems: [NewsItem] {
        items.sorted { $0.sortKey > $1.sortKey }
    }
}
