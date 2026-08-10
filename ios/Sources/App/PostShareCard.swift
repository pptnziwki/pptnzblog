import SwiftUI

/// 인스타그램 스토리 등 외부 공유용 3:4 비율 카드.
/// 실제 화면에는 그려지지 않고, `ImageRenderer`로 오프스크린 렌더링해 이미지로만 사용한다.
struct PostShareCard: View {
    let post: Post

    /// 3:4 비율. `ImageRenderer.scale`을 곱해 최종 픽셀 크기가 결정된다.
    static let size = CGSize(width: 360, height: 480)

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.pptnzBackground

            VStack(alignment: .leading, spacing: 20) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 28)

                Spacer()

                Text(post.title)
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(Color.pptnzPink)
                    .lineLimit(4)

                Text(post.content)
                    .font(.system(size: 15))
                    .foregroundStyle(Color.pptnzInk)
                    .lineLimit(6)

                Spacer()

                HStack {
                    Text(post.date)
                        .font(.system(size: 12))
                        .foregroundStyle(Color.pptnzInk.opacity(0.6))
                    Spacer()
                    Text("pptnz.net")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Color.pptnzCoral)
                }
            }
            .padding(24)
        }
        .frame(width: Self.size.width, height: Self.size.height)
    }
}

@MainActor
enum PostShareCardRenderer {
    /// SwiftUI 카드를 고해상도 PNG `UIImage`로 렌더링한다.
    static func render(post: Post) -> UIImage? {
        let renderer = ImageRenderer(content: PostShareCard(post: post))
        renderer.scale = 3
        return renderer.uiImage
    }
}
