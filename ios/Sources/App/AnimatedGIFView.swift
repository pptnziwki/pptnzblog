import SwiftUI
import ImageIO

/// 애니메이션 GIF를 프레임 단위로 디코딩해 재생하는 뷰.
/// SwiftUI `Image`/`AsyncImage`는 GIF의 첫 프레임만 정지 이미지로 보여주기 때문에 직접 구현했다.
struct AnimatedGIFView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> UIImageView {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.clipsToBounds = true
        loadAndAnimate(into: imageView)
        return imageView
    }

    func updateUIView(_ uiView: UIImageView, context: Context) {}

    private func loadAndAnimate(into imageView: UIImageView) {
        Task {
            guard let (data, _) = try? await URLSession.shared.data(from: url) else { return }
            let (images, duration) = Self.decodeFrames(from: data)
            await MainActor.run {
                guard !images.isEmpty else { return }
                imageView.image = images.first
                if images.count > 1 {
                    imageView.animationImages = images
                    imageView.animationDuration = duration
                    imageView.animationRepeatCount = 0
                    imageView.startAnimating()
                }
            }
        }
    }

    private static func decodeFrames(from data: Data) -> (images: [UIImage], duration: Double) {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return ([], 0) }
        let count = CGImageSourceGetCount(source)

        var images: [UIImage] = []
        var totalDuration: Double = 0
        for index in 0..<count {
            guard let cgImage = CGImageSourceCreateImageAtIndex(source, index, nil) else { continue }
            totalDuration += frameDuration(source: source, index: index)
            images.append(UIImage(cgImage: cgImage))
        }
        return (images, totalDuration > 0 ? totalDuration : Double(images.count) * 0.1)
    }

    private static func frameDuration(source: CGImageSource, index: Int) -> Double {
        guard let properties = CGImageSourceCopyPropertiesAtIndex(source, index, nil) as? [CFString: Any],
              let gifProperties = properties[kCGImagePropertyGIFDictionary] as? [CFString: Any] else {
            return 0.1
        }
        let delay = gifProperties[kCGImagePropertyGIFUnclampedDelayTime] as? Double
            ?? gifProperties[kCGImagePropertyGIFDelayTime] as? Double
            ?? 0.1
        return max(delay, 0.02)
    }
}
