import Foundation

/// 즐겨찾기(북마크)한 글 id를 UserDefaults에 저장해두는 간단한 스토어.
final class BookmarksStore: ObservableObject {
    static let shared = BookmarksStore()

    @Published private(set) var bookmarkedIDs: Set<String>

    private let defaultsKey = "bookmarkedPostIDs"

    private init() {
        bookmarkedIDs = Set(UserDefaults.standard.stringArray(forKey: defaultsKey) ?? [])
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
        UserDefaults.standard.set(Array(bookmarkedIDs), forKey: defaultsKey)
    }
}
