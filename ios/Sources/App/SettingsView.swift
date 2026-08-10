import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var notificationSettings = NotificationSettings.shared

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1"
        return "\(version) (\(build))"
    }

    var body: some View {
        NavigationStack {
            List {
                Section("앱 정보") {
                    LabeledContent("버전", value: appVersion)
                    LabeledContent("개발자", value: "Wooyoung Jang")
                    Link(destination: URL(string: "mailto:wooyxxng@gmail.com")!) {
                        LabeledContent("문의하기", value: "wooyxxng@gmail.com")
                    }
                    .foregroundStyle(.primary)
                }

                Section("페퍼톤스 공식 계정") {
                    accountLink("공식 홈페이지", systemImage: "globe", url: "https://peppertones.net")
                    accountLink("Instagram", systemImage: "camera", url: "https://www.instagram.com/peppertones_official")
                    accountLink("X (Twitter)", systemImage: "at", url: "https://x.com/pptnzexpress")
                }

                Section("알림") {
                    Toggle("알림 허용", isOn: $notificationSettings.notificationsEnabled)
                    if notificationSettings.notificationsEnabled {
                        DatePicker(
                            "매일 알림 시간",
                            selection: $notificationSettings.dailyTime,
                            displayedComponents: .hourAndMinute
                        )
                    }
                }
            }
            .navigationTitle("설정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
            .onChange(of: notificationSettings.notificationsEnabled) { _, enabled in
                if enabled {
                    BackgroundRefresh.requestNotificationPermissionIfNeeded()
                }
                Task { await DailyPostNotifier.reschedule(force: true) }
            }
            .onChange(of: notificationSettings.dailyTime) { _, _ in
                Task { await DailyPostNotifier.reschedule(force: true) }
            }
        }
    }

    private func accountLink(_ title: String, systemImage: String, url: String) -> some View {
        Link(destination: URL(string: url)!) {
            Label {
                Text(title).foregroundStyle(.primary)
            } icon: {
                Image(systemName: systemImage).foregroundStyle(Color.pptnzCoral)
            }
        }
    }
}

#Preview {
    SettingsView()
}
