import SwiftUI

struct PostDetailView: View {
    let post: Post

    @State private var detail: PostDetail?
    @State private var isLoading = true
    @State private var loadFailed = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(post.title)
                    .font(.title2.bold())
                    .foregroundStyle(Color.pptnzInk)
                Text(post.date)
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                bodyContent

                if let url = URL(string: post.link) {
                    Link("블로그에서 원문 보기", destination: url)
                        .font(.footnote.bold())
                        .foregroundStyle(Color.pptnzCoral)
                        .padding(.top, 4)
                }

                if let detail {
                    if !detail.links.isEmpty {
                        linksSection(detail.links)
                    }
                    commentsSection(detail.comments)
                }
            }
            .padding()
        }
        .background(Color.pptnzBackground)
        .navigationTitle(post.yearLabel)
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    @ViewBuilder
    private var bodyContent: some View {
        if let detail, !detail.blocks.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(detail.blocks.enumerated()), id: \.offset) { _, block in
                    switch block {
                    case .text(let text):
                        Text(text)
                            .font(.body)
                            .foregroundStyle(Color.pptnzInk)
                    case .image(let url):
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFit()
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            case .failure:
                                EmptyView()
                            default:
                                ProgressView()
                                    .frame(maxWidth: .infinity, minHeight: 120)
                            }
                        }
                    }
                }
            }
        } else if isLoading {
            ProgressView("원문을 불러오는 중…")
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
        } else {
            // 실시간 파싱에 실패해도 posts.json에 저장된 텍스트는 그대로 보여준다.
            Text(post.content)
                .font(.body)
                .foregroundStyle(Color.pptnzInk)
            if loadFailed {
                Text("원문 페이지를 불러오지 못해 요약 내용만 표시하고 있어요.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func linksSection(_ links: [URL]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("첨부 링크")
                .font(.headline)
                .foregroundStyle(Color.pptnzPink)
            ForEach(links, id: \.self) { url in
                Link(url.absoluteString, destination: url)
                    .font(.footnote)
                    .foregroundStyle(Color.pptnzCoral)
                    .lineLimit(1)
            }
        }
        .padding(.top, 8)
    }

    private func commentsSection(_ comments: [PostComment]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("댓글 \(comments.count)개")
                .font(.headline)
                .foregroundStyle(Color.pptnzPink)

            if comments.isEmpty {
                Text("아직 댓글이 없어요.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(comments) { comment in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(comment.author)
                                .font(.footnote.bold())
                                .foregroundStyle(Color.pptnzTeal)
                            if !comment.date.isEmpty {
                                Text(comment.date)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Text(comment.text)
                            .font(.subheadline)
                            .foregroundStyle(Color.pptnzInk)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .padding(.top, 8)
    }

    private func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            detail = try await PostDetailLoader.shared.load(for: post)
        } catch {
            loadFailed = true
        }
    }
}

#Preview {
    NavigationStack {
        PostDetailView(post: .preview)
    }
}
