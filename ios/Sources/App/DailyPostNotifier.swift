import Foundation
import UserNotifications

/// 매일 설정된 시각에 랜덤 글 1개를 로컬 알림으로 보낸다.
///
/// 이 앱은 서버 push 없이 로컬 알림만 쓰기 때문에, 예약 시점에 알림 내용이
/// 고정된다. 그래서 "매번 1회분(다음 발송 1건)"만 랜덤 글로 예약해두고,
/// 앱이 열리거나 백그라운드 새로고침이 돌 때마다 이미 지난 예약이면
/// 다음 날 몫을 새 글로 다시 예약하는 방식으로 "매일 다른 글"을 구현한다.
/// (앱을 아예 안 켜는 날이 길어지면 갱신이 늦어질 수 있다는 한계는 있다.)
enum DailyPostNotifier {
    static let requestIdentifier = "dailyRandomPost"
    private static let scheduledDateKey = "dailyRandomPost.scheduledDate"

    private static var sharedDefaults: UserDefaults? {
        UserDefaults(suiteName: PostsRepository.appGroupID)
    }

    /// 이미 예약된 발송이 아직 남아있으면 그대로 두고, 지났으면(또는 없으면) 새로 예약한다.
    static func rescheduleIfNeeded() async {
        await reschedule(force: false)
    }

    /// 설정(허용 여부/시각)이 바뀌었을 때는 즉시 다시 예약해야 하므로 force로 강제한다.
    static func reschedule(force: Bool) async {
        guard NotificationSettings.shared.notificationsEnabled else {
            cancel()
            return
        }

        if !force,
           let scheduled = sharedDefaults?.object(forKey: scheduledDateKey) as? Date,
           scheduled > .now {
            return
        }

        guard let posts = try? await PostsRepository.shared.loadPosts(), !posts.isEmpty,
              let post = posts.randomElement(),
              let fireDate = nextFireDate() else { return }

        let content = UNMutableNotificationContent()
        content.title = "pptnz.net"
        content.subtitle = "오늘의 글"
        content.body = post.title.isEmpty ? String(post.content.prefix(60)) : post.title
        content.sound = .default
        content.userInfo = ["postID": post.id]

        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: fireDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: requestIdentifier, content: content, trigger: trigger)

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [requestIdentifier])
        try? await UNUserNotificationCenter.current().add(request)
        sharedDefaults?.set(fireDate, forKey: scheduledDateKey)
    }

    static func cancel() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [requestIdentifier])
        sharedDefaults?.removeObject(forKey: scheduledDateKey)
    }

    /// 설정된 시:분 기준 다음 발송 시각(오늘 그 시각이 이미 지났으면 내일)을 계산.
    private static func nextFireDate() -> Date? {
        let settings = NotificationSettings.shared
        var components = Calendar.current.dateComponents([.year, .month, .day], from: .now)
        components.hour = settings.dailyHour
        components.minute = settings.dailyMinute
        components.second = 0
        guard let today = Calendar.current.date(from: components) else { return nil }
        return today > .now ? today : Calendar.current.date(byAdding: .day, value: 1, to: today)
    }
}
