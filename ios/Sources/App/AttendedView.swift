import SwiftUI

/// 다녀온 공연 모아보기. (블로그 북마크함과 같은 형태의 시트)
struct AttendedView: View {
    @ObservedObject private var attended = AttendedStore.shared
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var isExporting = false
    @State private var exportMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if attended.items.isEmpty {
                    ContentUnavailableView(
                        "다녀온 공연이 없어요",
                        systemImage: "ticket",
                        description: Text("NEWS 목록에서 공연 오른쪽 체크박스를 눌러 추가해보세요.")
                    )
                } else {
                    List(attended.sortedItems) { item in
                        NewsRowView(item: item) {
                            if let url = item.linkURL { openURL(url) }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .scrollContentBackground(.hidden)
                    .background(Color.pptnzBackground)
                }
            }
            .navigationTitle("다녀온 공연 \(attended.items.count > 0 ? "(\(attended.items.count))" : "")")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        Task { await exportImages() }
                    } label: {
                        if isExporting {
                            ProgressView()
                        } else {
                            Image(systemName: "square.and.arrow.down")
                        }
                    }
                    .disabled(attended.items.isEmpty || isExporting)
                    .accessibilityLabel("이미지로 저장")
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
        }
        .alert("이미지 저장", isPresented: Binding(
            get: { exportMessage != nil },
            set: { if !$0 { exportMessage = nil } }
        )) {
            Button("확인", role: .cancel) {}
        } message: {
            Text(exportMessage ?? "")
        }
    }

    /// 다녀온 공연 목록을 3:4 이미지(여러 장일 수 있음)로 만들어 사진 앱에 한 번에 저장한다.
    private func exportImages() async {
        isExporting = true
        defer { isExporting = false }
        let images = AttendedExporter.renderImages(items: attended.sortedItems)
        switch await AttendedExporter.saveToPhotos(images) {
        case .saved(let count):
            exportMessage = "\(count)장의 이미지를 사진 앱에 저장했어요."
        case .denied:
            exportMessage = "사진 저장 권한이 없어요. 설정 > 개인정보 보호 > 사진에서 이 앱의 '사진 추가' 권한을 허용해 주세요."
        case .failed:
            exportMessage = "이미지를 저장하지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }
}

#Preview {
    AttendedView()
}
