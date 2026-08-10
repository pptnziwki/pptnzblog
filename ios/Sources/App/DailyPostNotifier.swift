import Foundation
import UserNotifications

/// 매일 설정된 시각(최대 3개)마다 랜덤 글 1개를 로컬 알림으로 보낸다.
///
/// 이 앱은 서버 push 없이 로컬 알림만 쓰기 때문에, 예약 시점에 알림 내용이
/// 고정된다. 그래서 시각마다 "다음 발송 1건"만 랜덤 글로 예약해두고,
/// 앱이 열리거나 백그라운드 새로고침이 돌 때마다 이미 지난 예약이면
/// 다음 날 몫을 새 글로 다시 예약하는 방식으로 "매일 다른 글"을 구현한다.
/// (앱을 아예 안 켜는 날이 길어지면 갱신이 늦어질 수 있다는 한계는 있다.)
enum DailyPostNotifier {
    private static let identifierPrefix = "dailyRandomPost."
    private static let scheduledDatesKey = "dailyRandomPost.scheduledDates"

    private static var sharedDefaults: UserDefaults? {
        UserDefaults(suiteName: PostsRepository.appGroupID)
    }

    /// 이미 예약된 발송이 아직 남아있으면 그대로 두고, 지났으면(또는 없으면) 새로 예약한다.
    static func rescheduleIfNeeded() async {
        await reschedule(force: false)
    }

    /// 설정(허용 여부/시각 목록)이 바뀌었을 때는 즉시 다시 예약해야 하므로 force로 강제한다.
    static func reschedule(force: Bool) async {
        let times = NotificationSettings.shared.dailyTimes
        guard NotificationSettings.shared.notificationsEnabled, !times.isEmpty else {
            cancelAll()
            return
        }

        var scheduledDates = loadScheduledDates()

        // 삭제된 시각에 대한 예약은 정리한다.
        let validIdentifiers = Set(times.map(identifier(for:)))
        let staleIdentifiers = scheduledDates.keys.filter { !validIdentifiers.contains($0) }
        if !staleIdentifiers.isEmpty {
            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: staleIdentifiers)
            staleIdentifiers.forEach { scheduledDates.removeValue(forKey: $0) }
        }

        for time in times {
            let identifier = identifier(for: time)
            if !force, let scheduled = scheduledDates[identifier], scheduled > .now {
                continue
            }

            guard let posts = try? await PostsRepository.shared.loadPosts(), !posts.isEmpty,
                  let post = posts.randomElement(),
                  let fireDate = nextFireDate(hour: time.hour, minute: time.minute) else { continue }

            let content = UNMutableNotificationContent()
            content.title = "pptnz.net"
            content.subtitle = "오늘의 글"
            content.body = post.title
            content.sound = .default
            content.userInfo = ["postID": post.id]

            let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: fireDate)
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
            try? await UNUserNotificationCenter.current().add(request)
            scheduledDates[identifier] = fireDate
        }

        saveScheduledDates(scheduledDates)
    }

    static func cancelAll() {
        let scheduledDates = loadScheduledDates()
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: Array(scheduledDates.keys))
        sharedDefaults?.removeObject(forKey: scheduledDatesKey)
    }

    private static func identifier(for time: NotificationSettings.DailyTime) -> String {
        identifierPrefix + time.id.uuidString
    }

    /// 설정된 시:분 기준 다음 발송 시각(오늘 그 시각이 이미 지났으면 내일)을 계산.
    private static func nextFireDate(hour: Int, minute: Int) -> Date? {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: .now)
        components.hour = hour
        components.minute = minute
        components.second = 0
        guard let today = Calendar.current.date(from: components) else { return nil }
        return today > .now ? today : Calendar.current.date(byAdding: .day, value: 1, to: today)
    }

    private static func loadScheduledDates() -> [String: Date] {
        guard let data = sharedDefaults?.data(forKey: scheduledDatesKey),
              let decoded = try? JSONDecoder().decode([String: Date].self, from: data) else { return [:] }
        return decoded
    }

    private static func saveScheduledDates(_ dates: [String: Date]) {
        guard let data = try? JSONEncoder().encode(dates) else { return }
        sharedDefaults?.set(data, forKey: scheduledDatesKey)
    }
}
