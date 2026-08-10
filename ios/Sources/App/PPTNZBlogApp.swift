import SwiftUI

@main
struct PPTNZBlogApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    BackgroundRefresh.requestNotificationPermissionIfNeeded()
                    BackgroundRefresh.scheduleNext()
                }
        }
        .backgroundTask(.appRefresh(BackgroundRefresh.taskIdentifier)) {
            await BackgroundRefresh.run()
        }
    }
}
