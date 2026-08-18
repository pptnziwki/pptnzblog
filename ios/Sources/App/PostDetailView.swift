import SwiftUI

struct PostDetailView: View {
    let post: Post

    @State private var detail: PostDetail?
    @State private var isLoading = true
    @State private var loadFailed = false
    @State private var showsAllComments = false
    @State private var shareItems: [Any]?
    @State private var showsTextPicker = false
    @ObservedObject private var bookmarks = BookmarksStore.shared

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(post.title)
                    .font(.title2.bold())
                    .foregroundStyle(Color.pptnzPink)
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
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .background(Color.pptnzBackground)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(height: 40)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    bookmarks.toggle(post)
                } label: {
                    Image(systemName: bookmarks.isBookmarked(post) ? "bookmark.fill" : "bookmark")
                        .foregroundStyle(Color.pptnzCoral)
                }
                .buttonStyle(.plain)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        startInstagramShare()
                    } label: {
                        Label("구간 선택", systemImage: "camera.viewfinder")
                    }
                    Button {
                        shareLink()
                    } label: {
                        Label("링크 공유", systemImage: "link")
                    }
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .foregroundStyle(Color.pptnzCoral)
                }
            }
        }
        .task { await load() }
        .sheet(isPresented: Binding(
            get: { shareItems != nil },
            set: { if !$0 { shareItems = nil } }
        )) {
            if let shareItems {
                ShareSheet(items: shareItems)
            }
        }
        .sheet(isPresented: $showsTextPicker) {
            ShareTextPickerView(
                paragraphs: ShareParagraphExtractor.paragraphs(from: detail?.blocks ?? [])
            ) { selectedText in
                shareToInstagramStory(displayText: selectedText)
            }
        }
    }

    /// 인스타그램 스토리 공유 흐름을 시작한다. 원문 파싱에 성공해 문단이 있으면
    /// 구간 선택 시트를 먼저 띄우고, 실패/로딩 중이면 post.content 그대로 카드에 담는다.
    private func startInstagramShare() {
        let paragraphs = ShareParagraphExtractor.paragraphs(from: detail?.blocks ?? [])
        guard !paragraphs.isEmpty else {
            shareToInstagramStory(displayText: nil)
            return
        }
        showsTextPicker = true
    }

    /// 3:4 카드를 렌더링해 인스타그램 스토리로 바로 공유하고,
    /// 인스타그램이 없으면 카드 이미지 + 글 링크로 일반 공유 시트를 띄운다.
    private func shareToInstagramStory(displayText: String?) {
        guard let image = PostShareCardRenderer.render(post: post, displayText: displayText) else { return }
        if InstagramStorySharer.share(image: image) { return }

        var items: [Any] = [image]
        if let url = URL(string: post.link) {
            items.append(url)
        }
        shareItems = items
    }

    /// 글 상세로 딥링크되는 공유 페이지 URL을 표준 공유 시트로 공유한다.
    /// 카카오톡/메시지 등에서는 OG 태그로 썸네일/제목/본문 미리보기가 뜨고,
    /// 앱이 설치된 기기에서 열면 해당 글로, 없으면 원문 블로그로 이동한다.
    private func shareLink() {
        guard let url = SharePagesConfig.url(for: post.id) else { return }
        shareItems = [url]
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
                        if url.pathExtension.lowercased() == "gif" {
                            AnimatedGIFView(url: url)
                                .frame(maxWidth: .infinity)
                                .frame(height: 220)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        } else {
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
                    case .youtube(let videoID):
                        VStack(alignment: .leading, spacing: 6) {
                            YouTubePlayerView(videoID: videoID)
                                .frame(maxWidth: .infinity)
                                .aspectRatio(16.0 / 9.0, contentMode: .fit)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            if let url = URL(string: "https://www.youtube.com/watch?v=\(videoID)") {
                                Link("유튜브에서 보기", destination: url)
                                    .font(.footnote.bold())
                                    .foregroundStyle(Color.pptnzCoral)
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
        let visibleComments = showsAllComments ? comments : Array(comments.prefix(3))

        return VStack(alignment: .leading, spacing: 12) {
            Text("댓글 \(comments.count)개")
                .font(.headline)
                .foregroundStyle(Color.pptnzPink)

            if comments.isEmpty {
                Text("아직 댓글이 없어요.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(visibleComments) { comment in
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

                if comments.count > 3 {
                    Button {
                        showsAllComments.toggle()
                    } label: {
                        Text(showsAllComments ? "댓글 접기" : "댓글 \(comments.count - 3)개 더보기")
                            .font(.footnote.bold())
                            .foregroundStyle(Color.pptnzCoral)
                    }
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
