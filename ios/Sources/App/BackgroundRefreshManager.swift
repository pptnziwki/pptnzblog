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

    /// 다음 백그라운드 새로고침을 예약한다. "1시간 간격"을 힌트로 주지만, iOS가 배터리/사용
    /// 패턴을 보고 실제 실행 시점을 정하므로 earliestBeginDate는 어디까지나 힌트일 뿐,
    /// 정확히 정각/그 시각에 실행된다는 보장은 없다(서버 push 없이 로컬에서 할 수 있는 최선).
    static func scheduleNext() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Calendar.current.date(byAdding: .hour, value: 1, to: .now)
        try? BGTaskScheduler.shared.submit(request)
    }

    /// `.backgroundTask(.appRefresh(taskIdentifier))`에서 호출.
    static func run() async {
        scheduleNext()
        await checkForNewPosts()
        await DailyPostNotifier.rescheduleIfNeeded()
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
        sharedDefaults?.set(latest.numericID, forKey: lastSeenIDKey)

        guard !newPosts.isEmpty else { return }
        // 알림이 꺼져 있어도 lastSeenID는 갱신해서, 나중에 다시 켰을 때 밀린 글이
        // 한꺼번에 알림으로 쏟아지지 않게 한다.
        guard NotificationSettings.shared.notificationsEnabled else { return }

        notify(newPosts: newPosts)
    }

    private static func notify(newPosts: [Post]) {
        guard let latest = newPosts.first else { return }

        let content = UNMutableNotificationContent()
        content.title = "새로운 글이 올라왔어요!"
        content.body = latest.content
        content.sound = .default
        content.userInfo = ["postID": latest.id]

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
