import SwiftUI

/// 인스타그램 스토리 등 외부 공유용 3:4 비율 카드.
/// 실제 화면에는 그려지지 않고, `ImageRenderer`로 오프스크린 렌더링해 이미지로만 사용한다.
struct PostShareCard: View {
    let post: Post

    /// 실제로 눈에 보이는 카드 영역(3:4 비율).
    static let cardSize = CGSize(width: 320, height: 427)
    /// 그림자가 잘리지 않도록 카드 바깥에 투명 여백을 둔다.
    private static let shadowPadding: CGFloat = 24
    /// `ImageRenderer`가 렌더링할 전체 캔버스 크기(카드 + 그림자 여백).
    static var size: CGSize {
        CGSize(
            width: cardSize.width + shadowPadding * 2,
            height: cardSize.height + shadowPadding * 2
        )
    }

    var body: some View {
        ZStack {
            Color.clear

            VStack(alignment: .leading, spacing: 14) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 26)

                Text(post.title)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(Color.pptnzPink)
                    .lineLimit(3)

                Text(post.content)
                    .font(.system(size: 14))
                    .foregroundStyle(Color.pptnzInk)
                    .lineLimit(14)

                Spacer(minLength: 0)

                HStack {
                    Text(post.date)
                        .font(.system(size: 11))
                        .foregroundStyle(Color.pptnzInk.opacity(0.6))
                    Spacer()
                    Text("pptnz.net")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color.pptnzCoral)
                }
            }
            .padding(24)
            .frame(width: Self.cardSize.width, height: Self.cardSize.height, alignment: .topLeading)
            .background(Color.pptnzBackground)
            .clipShape(RoundedRectangle(cornerRadius: 28))
            .shadow(color: .black.opacity(0.18), radius: 16, y: 6)
        }
        .frame(width: Self.size.width, height: Self.size.height)
    }
}

@MainActor
enum PostShareCardRenderer {
    /// SwiftUI 카드를 고해상도 PNG `UIImage`로 렌더링한다. 카드 밖 배경은 투명 처리된다.
    static func render(post: Post) -> UIImage? {
        let renderer = ImageRenderer(content: PostShareCard(post: post))
        renderer.scale = 3
        renderer.isOpaque = false
        return renderer.uiImage
    }
}
