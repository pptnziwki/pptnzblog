import SwiftUI
import UserNotifications

@main
struct PPTNZBlogApp: App {
    init() {
        UNUserNotificationCenter.current().delegate = NotificationDelegate.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    if NotificationSettings.shared.notificationsEnabled {
                        BackgroundRefresh.requestNotificationPermissionIfNeeded()
                    }
                    BackgroundRefresh.scheduleNext()
                    Task { await DailyPostNotifier.rescheduleIfNeeded() }
                }
                .onOpenURL { url in
                    // 위젯 탭으로 열릴 때 pptnzblog://post/{id} 형태로 들어온다.
                    guard url.scheme == "pptnzblog", url.host == "post" else { return }
                    NotificationDelegate.shared.pendingPostID = url.lastPathComponent
                }
        }
        .backgroundTask(.appRefresh(BackgroundRefresh.taskIdentifier)) {
            await BackgroundRefresh.run()
        }
    }
}
