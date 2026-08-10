import Foundation
import WidgetKit

/// 즐겨찾기(북마크)한 글 id를 App Group에 저장해 앱과 위젯이 함께 읽고 쓴다.
/// 북마크함을 보여주는 위젯 옵션이 있어서, 앱 UserDefaults가 아니라
/// PostsRepository와 같은 App Group 컨테이너에 저장해야 위젯에서도 보인다.
final class BookmarksStore: ObservableObject {
    static let shared = BookmarksStore()

    @Published private(set) var bookmarkedIDs: Set<String>

    private let defaultsKey = "bookmarkedPostIDs"
    private let defaults = UserDefaults(suiteName: PostsRepository.appGroupID)

    private init() {
        bookmarkedIDs = Set(defaults?.stringArray(forKey: defaultsKey) ?? [])
    }

    func isBookmarked(_ post: Post) -> Bool {
        bookmarkedIDs.contains(post.id)
    }

    func toggle(_ post: Post) {
        if bookmarkedIDs.contains(post.id) {
            bookmarkedIDs.remove(post.id)
        } else {
            bookmarkedIDs.insert(post.id)
        }
        defaults?.set(Array(bookmarkedIDs), forKey: defaultsKey)
        // 북마크함을 노출하는 위젯이 있을 수 있으니 즉시 갱신을 요청한다.
        WidgetCenter.shared.reloadTimelines(ofKind: PostsRepository.widgetKind)
    }
}
