import Foundation

/// 알림 허용 여부, 매일 랜덤 글 알림을 보낼 시각(최대 3개)을 저장하는 설정 스토어.
final class NotificationSettings: ObservableObject {
    static let shared = NotificationSettings()
    static let maxDailyTimes = 3

    struct DailyTime: Codable, Identifiable, Hashable {
        var id = UUID()
        var hour: Int
        var minute: Int
    }

    private static let enabledKey = "notifications.enabled"
    private static let timesKey = "notifications.dailyTimes"

    private let defaults = UserDefaults(suiteName: PostsRepository.appGroupID)

    @Published var notificationsEnabled: Bool {
        didSet { defaults?.set(notificationsEnabled, forKey: Self.enabledKey) }
    }

    @Published var dailyTimes: [DailyTime] {
        didSet { saveDailyTimes() }
    }

    private init() {
        let defaults = UserDefaults(suiteName: PostsRepository.appGroupID)
        notificationsEnabled = defaults?.object(forKey: Self.enabledKey) as? Bool ?? true
        if let data = defaults?.data(forKey: Self.timesKey),
           let decoded = try? JSONDecoder().decode([DailyTime].self, from: data) {
            dailyTimes = decoded
        } else {
            dailyTimes = [DailyTime(hour: 9, minute: 0)]
        }
    }

    func addDailyTime() {
        guard dailyTimes.count < Self.maxDailyTimes else { return }
        dailyTimes.append(DailyTime(hour: 9, minute: 0))
    }

    func removeDailyTime(_ id: UUID) {
        dailyTimes.removeAll { $0.id == id }
    }

    /// DatePicker(.hourAndMinute)에 바로 바인딩할 수 있도록 시:분을 Date로 감싸서 노출.
    func time(for id: UUID) -> Date {
        guard let time = dailyTimes.first(where: { $0.id == id }) else { return .now }
        var components = Calendar.current.dateComponents([.year, .month, .day], from: .now)
        components.hour = time.hour
        components.minute = time.minute
        return Calendar.current.date(from: components) ?? .now
    }

    func setTime(_ date: Date, for id: UUID) {
        guard let index = dailyTimes.firstIndex(where: { $0.id == id }) else { return }
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        dailyTimes[index].hour = components.hour ?? dailyTimes[index].hour
        dailyTimes[index].minute = components.minute ?? dailyTimes[index].minute
    }

    private func saveDailyTimes() {
        guard let data = try? JSONEncoder().encode(dailyTimes) else { return }
        defaults?.set(data, forKey: Self.timesKey)
    }
}
