import UIKit

/// 인스타그램 스토리에 이미지를 스티커로 직접 공유한다.
/// 공식적으로 문서화된 API가 아니라 인스타그램이 지원하는 pasteboard 기반 URL scheme 연동이며,
/// (`instagram-stories://share`), 인스타그램이 설치돼 있지 않으면 아무 동작도 하지 않는다.
enum InstagramStorySharer {
    private static let urlScheme = "instagram-stories://share"

    static var isAvailable: Bool {
        guard let url = URL(string: urlScheme) else { return false }
        return UIApplication.shared.canOpenURL(url)
    }

    /// 인스타그램 스토리 작성 화면으로 바로 이동하며 카드 이미지를 스티커로 전달한다.
    /// 인스타그램이 설치돼 있지 않으면 false를 반환해 호출부가 일반 공유 시트로 대체하게 한다.
    @discardableResult
    static func share(image: UIImage) -> Bool {
        guard isAvailable,
              let imageData = image.pngData(),
              let sourceApplication = Bundle.main.bundleIdentifier,
              let url = URL(string: "\(urlScheme)?source_application=\(sourceApplication)") else {
            return false
        }

        let pasteboardItems: [String: Any] = [
            "com.instagram.sharedSticker.stickerImage": imageData,
            "com.instagram.sharedSticker.backgroundTopColor": "#FFFFFA",
            "com.instagram.sharedSticker.backgroundBottomColor": "#F6BBB7",
        ]
        let options = [UIPasteboard.OptionsKey.expirationDate: Date().addingTimeInterval(60 * 5)]
        UIPasteboard.general.setItems([pasteboardItems], options: options)

        UIApplication.shared.open(url)
        return true
    }
}
