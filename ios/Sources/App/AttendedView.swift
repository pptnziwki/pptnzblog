import SwiftUI

/// 다녀온 공연 모아보기. (블로그 북마크함과 같은 형태의 시트)
struct AttendedView: View {
    @ObservedObject private var attended = AttendedStore.shared
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

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
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
        }
    }
}

#Preview {
    AttendedView()
}
