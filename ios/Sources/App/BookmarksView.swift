import SwiftUI

struct BookmarksView: View {
    let posts: [Post]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if posts.isEmpty {
                    ContentUnavailableView("저장한 글이 없어요", systemImage: "bookmark")
                } else {
                    List(posts) { post in
                        NavigationLink(value: post) {
                            PostRowView(post: post)
                        }
                    }
                    .listStyle(.insetGrouped)
                    .scrollContentBackground(.hidden)
                    .background(Color.pptnzBackground)
                }
            }
            .navigationTitle("북마크함")
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: Post.self) { post in
                PostDetailView(post: post)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
        }
    }
}

#Preview {
    BookmarksView(posts: [.preview])
}
