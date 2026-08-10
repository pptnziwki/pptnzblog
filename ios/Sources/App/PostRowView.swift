import SwiftUI

struct PostRowView: View {
    let post: Post

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(post.title)
                .font(.headline)
                .foregroundStyle(Color.pptnzInk)
                .lineLimit(2)
            Text(post.date)
                .font(.caption)
                .foregroundStyle(Color.pptnzTeal)
            if !post.content.isEmpty {
                Text(post.content)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 4)
        .listRowBackground(Color.pptnzBackground)
    }
}
