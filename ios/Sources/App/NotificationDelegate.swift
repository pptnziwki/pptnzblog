import Foundation
import UserNotifications

/// 알림을 탭했을 때 어떤 글로 이동해야 하는지 SwiftUI 쪽에 전달한다.
final class NotificationDelegate: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    /// 알림 탭으로 열어야 할 글 id. ContentView가 이 값을 관찰해서 상세 화면으로 이동시킨다.
    @Published var pendingPostID: String?

    private override init() {}

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let postID = response.notification.request.content.userInfo["postID"] as? String {
            pendingPostID = postID
        }
        completionHandler()
    }

    /// 앱이 포그라운드에 떠 있을 때도 알림 배너가 보이도록 한다.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .list])
    }
}
