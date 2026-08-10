import SwiftUI

/// 페퍼톤스 공식 블로그(skin `pptnz8`) CSS에서 추출한 색상 팔레트.
/// `http://peppertones.host.whoisweb.net/blog/skin/pptnz8/style.css` 기준.
extension Color {
    /// 페이지 배경 (#FFFFFA)
    static let pptnzBackground = Color(red: 1.0, green: 1.0, blue: 250.0 / 255.0)
    /// 본문 텍스트 (#101030)
    static let pptnzInk = Color(red: 16.0 / 255.0, green: 16.0 / 255.0, blue: 48.0 / 255.0)
    /// 링크·강조 포인트 (#E88580)
    static let pptnzCoral = Color(red: 232.0 / 255.0, green: 133.0 / 255.0, blue: 128.0 / 255.0)
    /// 보조 강조 (#7AAAAA)
    static let pptnzTeal = Color(red: 122.0 / 255.0, green: 170.0 / 255.0, blue: 170.0 / 255.0)
    /// 섹션 제목류 (#F6BBB7)
    static let pptnzPink = Color(red: 246.0 / 255.0, green: 187.0 / 255.0, blue: 183.0 / 255.0)
}
