import Foundation
import UserNotifications

/// news 탭에 새 소식이 올라왔는지 감지해 로컬 알림을 띄운다.
/// 블로그 새 글 알림(BackgroundRefresh)과 같은 방식으로 Background App Refresh 때 호출된다.
///
/// 블로그는 글 id가 숫자라서 "가장 큰 id"만 기억하면 되지만, news는 고유 번호가 없고
/// 내용 해시가 id라서 "이미 본 id 집합"을 저장해두고 거기에 없는 항목을 새 소식으로 본다.
enum NewsNotifier {
    private static let seenIDsKey = "seenNewsIDs"

    private static var sharedDefaults: UserDefaults? {
        UserDefaults(suiteName: PostsRepository.appGroupID)
    }

    /// 백그라운드 새로고침에서 호출. 새 소식이 있으면 알림을 보낸다.
    static func checkForNew() async {
        guard let items = try? await NewsRepository.shared.loadNews(), !items.isEmpty else { return }

        guard let seen = loadSeenIDs() else {
            // 최초 실행: 기존 소식 수백 건이 알림으로 쏟아지지 않도록 기준점만 저장한다.
            saveSeenIDs(Set(items.map(\.id)))
            return
        }

        let newItems = items.filter { !seen.contains($0.id) }
        guard !newItems.isEmpty else { return }

        // 알림이 꺼져 있어도 본 것으로 처리해서, 나중에 다시 켰을 때 밀린 소식이 한꺼번에 오지 않게 한다.
        saveSeenIDs(seen.union(items.map(\.id)))
        guard NotificationSettings.shared.notificationsEnabled else { return }

        notify(newItems: newItems)
    }

    /// 사용자가 NEWS 탭에서 목록을 직접 본 경우, 그 소식들은 이미 본 것으로 기록한다.
    /// (이미 본 소식이 나중에 백그라운드 새로고침에서 "새 소식" 알림으로 오는 걸 막는다.)
    static func markSeen(_ items: [NewsItem]) {
        guard !items.isEmpty else { return }
        saveSeenIDs((loadSeenIDs() ?? []).union(items.map(\.id)))
    }

    private static func notify(newItems: [NewsItem]) {
        guard let first = newItems.first else { return }

        let content = UNMutableNotificationContent()
        content.title = "새로운 소식이 올라왔어요!"
        let summary = "[\(first.tag.uppercased()) \(first.date)] \(first.content.replacingOccurrences(of: "\n", with: " "))"
        content.body = newItems.count > 1 ? "\(summary) 외 \(newItems.count - 1)건" : summary
        content.sound = .default
        content.userInfo = ["newsID": first.id]

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    private static func loadSeenIDs() -> Set<String>? {
        guard let array = sharedDefaults?.stringArray(forKey: seenIDsKey) else { return nil }
        return Set(array)
    }

    private static func saveSeenIDs(_ ids: Set<String>) {
        sharedDefaults?.set(Array(ids), forKey: seenIDsKey)
    }
}
