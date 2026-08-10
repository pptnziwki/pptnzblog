import SwiftUI

struct PostDetailView: View {
    let post: Post

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(post.title)
                    .font(.title2.bold())
                Text(post.date)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                Text(post.content)
                    .font(.body)
                if let url = URL(string: post.link) {
                    Link("블로그에서 원문 보기", destination: url)
                        .padding(.top, 8)
                }
            }
            .padding()
        }
        .navigationTitle(post.yearLabel)
        .navigationBarTitleDisplayMode(.inline)
    }
}
