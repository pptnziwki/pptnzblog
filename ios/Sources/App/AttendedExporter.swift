import Photos
import SwiftUI
import UIKit

/// 다녀온 공연 목록을 3:4 비율 이미지로 내보낸다.
/// 한 장에 다 들어가지 않으면 여러 장으로 나눠서 만든다.
enum AttendedExporter {
    /// 한 장의 논리 크기(3:4). 3배 스케일로 렌더링해서 1080x1440 이미지가 된다.
    static let pageSize = CGSize(width: 360, height: 480)

    enum Layout {
        static let padding: CGFloat = 22
        static let headerHeight: CGFloat = 40
        static let footerHeight: CGFloat = 16
        static let sectionSpacing: CGFloat = 12
        static let rowSpacing: CGFloat = 10
        static let dateWidth: CGFloat = 66
        static let columnSpacing: CGFloat = 8
        static let contentFont = UIFont.systemFont(ofSize: 13)
        static let maxLines = 3

        /// 항목들이 들어갈 수 있는 세로 영역.
        static var bodyHeight: CGFloat {
            pageSize.height - padding * 2 - headerHeight - footerHeight - sectionSpacing * 2
        }

        /// 본문 글자가 차지하는 가로 폭.
        static var contentWidth: CGFloat {
            pageSize.width - padding * 2 - dateWidth - columnSpacing
        }
    }

    // MARK: - 페이지 나누기

    /// 항목 하나가 차지하는 높이. 화면에 그려질 글자와 같은 폰트로 줄 수를 직접 재서,
    /// 렌더링 결과와 어긋나지 않게 한다.
    static func rowHeight(for item: NewsItem) -> CGFloat {
        let font = Layout.contentFont
        let bounds = (item.content as NSString).boundingRect(
            with: CGSize(width: Layout.contentWidth, height: .greatestFiniteMagnitude),
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: [.font: font],
            context: nil
        )
        let maxHeight = font.lineHeight * CGFloat(Layout.maxLines)
        return ceil(min(bounds.height, maxHeight)) + 2
    }

    /// 항목을 순서대로 채우다가 넘치면 다음 페이지로 넘긴다.
    static func paginate(_ items: [NewsItem]) -> [[NewsItem]] {
        var pages: [[NewsItem]] = []
        var current: [NewsItem] = []
        var used: CGFloat = 0
        for item in items {
            let height = rowHeight(for: item)
            let needed = current.isEmpty ? height : height + Layout.rowSpacing
            if !current.isEmpty, used + needed > Layout.bodyHeight {
                pages.append(current)
                current = []
                used = 0
            }
            used += current.isEmpty ? height : height + Layout.rowSpacing
            current.append(item)
        }
        if !current.isEmpty { pages.append(current) }
        return pages
    }

    // MARK: - 렌더링

    @MainActor
    static func renderImages(items: [NewsItem]) -> [UIImage] {
        let pages = paginate(items)
        return pages.enumerated().compactMap { index, pageItems in
            let renderer = ImageRenderer(
                content: AttendedExportPage(
                    items: pageItems,
                    totalCount: items.count,
                    page: index + 1,
                    pageCount: pages.count
                )
            )
            renderer.scale = 3
            return renderer.uiImage
        }
    }

    // MARK: - 사진 앱에 저장

    enum SaveResult {
        case saved(Int)
        case denied
        case failed
    }

    /// 만든 이미지를 한 번에 사진 앱에 저장한다. (추가 전용 권한만 요청)
    static func saveToPhotos(_ images: [UIImage]) async -> SaveResult {
        guard !images.isEmpty else { return .failed }
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else { return .denied }
        do {
            try await PHPhotoLibrary.shared().performChanges {
                for image in images {
                    PHAssetChangeRequest.creationRequestForAsset(from: image)
                }
            }
            return .saved(images.count)
        } catch {
            return .failed
        }
    }
}

/// 내보내기 이미지 한 장. 실제 화면에는 쓰이지 않고 `ImageRenderer`로만 그린다.
struct AttendedExportPage: View {
    let items: [NewsItem]
    let totalCount: Int
    let page: Int
    let pageCount: Int

    private typealias Layout = AttendedExporter.Layout

    var body: some View {
        VStack(alignment: .leading, spacing: Layout.sectionSpacing) {
            header
                .frame(height: Layout.headerHeight, alignment: .topLeading)

            VStack(alignment: .leading, spacing: Layout.rowSpacing) {
                ForEach(items) { item in
                    HStack(alignment: .top, spacing: Layout.columnSpacing) {
                        Text(item.date)
                            .font(.system(size: 11))
                            .foregroundStyle(Color.pptnzInk.opacity(0.55))
                            .frame(width: Layout.dateWidth, alignment: .leading)
                            .padding(.top, 1)
                        Text(item.content)
                            .font(.system(size: Layout.contentFont.pointSize))
                            .foregroundStyle(Color.pptnzInk)
                            .lineLimit(Layout.maxLines)
                            .frame(width: Layout.contentWidth, alignment: .leading)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: Layout.bodyHeight, alignment: .topLeading)

            footer
                .frame(height: Layout.footerHeight)
        }
        .padding(Layout.padding)
        .frame(width: AttendedExporter.pageSize.width, height: AttendedExporter.pageSize.height, alignment: .topLeading)
        .background(Color.pptnzBackground)
    }

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 20)
                Text("다녀온 공연 \(totalCount)")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(Color.pptnzPink)
            }
            Spacer()
            if pageCount > 1 {
                Text("\(page) / \(pageCount)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color.pptnzInk.opacity(0.55))
            }
        }
    }

    private var footer: some View {
        HStack {
            Spacer()
            Text("pptnz.net")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(Color.pptnzCoral)
        }
    }
}
