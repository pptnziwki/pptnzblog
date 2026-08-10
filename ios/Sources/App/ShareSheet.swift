import SwiftUI

/// 인스타그램이 없을 때(또는 사용자가 다른 앱으로 공유하고 싶을 때) 쓰는 일반 iOS 공유 시트.
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
