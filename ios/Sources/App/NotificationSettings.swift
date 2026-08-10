import Foundation

/// 알림 허용 여부, 매일 랜덤 글 알림을 보낼 시각을 저장하는 설정 스토어.
final class NotificationSettings: ObservableObject {
    static let shared = NotificationSettings()

    private static let enabledKey = "notifications.enabled"
    private static let hourKey = "notifications.dailyHour"
    private static let minuteKey = "notifications.dailyMinute"

    private let defaults = UserDefaults(suiteName: PostsRepository.appGroupID)

    @Published var notificationsEnabled: Bool {
        didSet { defaults?.set(notificationsEnabled, forKey: Self.enabledKey) }
    }

    @Published var dailyHour: Int {
        didSet { defaults?.set(dailyHour, forKey: Self.hourKey) }
    }

    @Published var dailyMinute: Int {
        didSet { defaults?.set(dailyMinute, forKey: Self.minuteKey) }
    }

    private init() {
        let defaults = UserDefaults(suiteName: PostsRepository.appGroupID)
        notificationsEnabled = defaults?.object(forKey: Self.enabledKey) as? Bool ?? true
        dailyHour = defaults?.object(forKey: Self.hourKey) as? Int ?? 9
        dailyMinute = defaults?.object(forKey: Self.minuteKey) as? Int ?? 0
    }

    /// DatePicker(.hourAndMinute)에 바로 바인딩할 수 있도록 시:분을 Date로 감싸서 노출.
    var dailyTime: Date {
        get {
            var components = Calendar.current.dateComponents([.year, .month, .day], from: .now)
            components.hour = dailyHour
            components.minute = dailyMinute
            return Calendar.current.date(from: components) ?? .now
        }
        set {
            let components = Calendar.current.dateComponents([.hour, .minute], from: newValue)
            dailyHour = components.hour ?? 9
            dailyMinute = components.minute ?? 0
        }
    }
}
