import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss

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
                    LabeledContent("개발자", value: "wooyxxng")
                    Link(destination: URL(string: "mailto:wooyxxng@gmail.com")!) {
                        LabeledContent("문의하기", value: "wooyxxng@gmail.com")
                    }
                }

                Section("페퍼톤스 공식 계정") {
                    Link(destination: URL(string: "https://peppertones.net")!) {
                        Label("공식 홈페이지", systemImage: "globe")
                    }
                    Link(destination: URL(string: "https://www.instagram.com/peppertones_official")!) {
                        Label("Instagram", systemImage: "camera")
                    }
                    Link(destination: URL(string: "https://x.com/pptnzexpress")!) {
                        Label("X (Twitter)", systemImage: "at")
                    }
                }
            }
            .tint(Color.pptnzCoral)
            .navigationTitle("설정")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
        }
    }
}

#Preview {
    SettingsView()
}
