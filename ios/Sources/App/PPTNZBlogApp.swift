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
        }
        .backgroundTask(.appRefresh(BackgroundRefresh.taskIdentifier)) {
            await BackgroundRefresh.run()
        }
    }
}
