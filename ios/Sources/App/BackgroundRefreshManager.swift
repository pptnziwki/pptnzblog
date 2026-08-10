import BackgroundTasks
import Foundation
import UserNotifications

/// Background App Refresh로 새 글을 감지해 로컬 알림을 띄운다.
/// 예전에는 GitHub Actions → Pushcut 웹훅으로 처리했지만, 서버/외부 서비스 없이
/// 앱 자체 백그라운드 새로고침만으로 동작하도록 대체했다.
enum BackgroundRefresh {
    static let taskIdentifier = "com.wooyxxng.pptnzblog.refresh"

    private static let lastSeenIDKey = "lastSeenPostID"

    /// PostsRepository와 동일한 App Group. 앱/위젯/백그라운드 작업이 모두 이 값을 공유해야 한다.
    private static let sharedDefaults = UserDefaults(suiteName: PostsRepository.appGroupID)

    static func requestNotificationPermissionIfNeeded() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    /// 다음 백그라운드 새로고침을 예약한다. iOS가 배터리/사용 패턴을 보고 실제 실행 시점을 정하므로
    /// earliestBeginDate는 어디까지나 힌트일 뿐, 정확히 그 시각에 실행된다는 보장은 없다.
    static func scheduleNext() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Calendar.current.date(byAdding: .minute, value: 30, to: .now)
        try? BGTaskScheduler.shared.submit(request)
    }

    /// `.backgroundTask(.appRefresh(taskIdentifier))`에서 호출.
    static func run() async {
        scheduleNext()
        await checkForNewPosts()
    }

    private static func checkForNewPosts() async {
        guard let posts = try? await PostsRepository.shared.loadPosts(), !posts.isEmpty else { return }

        let sorted = posts.sorted { $0.numericID > $1.numericID }
        guard let latest = sorted.first else { return }

        let lastSeenID = sharedDefaults?.integer(forKey: lastSeenIDKey) ?? 0

        guard lastSeenID > 0 else {
            // 최초 실행: 스팸성 알림을 막기 위해 기준점만 저장하고 알림은 보내지 않는다.
            sharedDefaults?.set(latest.numericID, forKey: lastSeenIDKey)
            return
        }

        let newPosts = sorted.filter { $0.numericID > lastSeenID }
        guard !newPosts.isEmpty else { return }

        notify(newPosts: newPosts)
        sharedDefaults?.set(latest.numericID, forKey: lastSeenIDKey)
    }

    private static func notify(newPosts: [Post]) {
        let content = UNMutableNotificationContent()
        if newPosts.count == 1, let post = newPosts.first {
            content.title = "PPTNZ 블로그 새 글"
            content.body = post.title
        } else {
            content.title = "PPTNZ 블로그 새 글 \(newPosts.count)건"
            content.body = newPosts.prefix(3).map(\.title).joined(separator: ", ")
        }
        content.sound = .default

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
