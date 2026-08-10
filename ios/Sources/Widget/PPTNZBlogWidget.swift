import WidgetKit
import SwiftUI
import AppIntents

/// 위젯에 어떤 글을 보여줄지: 매번 랜덤으로 고르거나, 북마크함에서만 고른다.
enum WidgetPostSource: String, AppEnum {
    case random
    case bookmarks

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "글 노출 방식"
    static var caseDisplayRepresentations: [WidgetPostSource: DisplayRepresentation] = [
        .random: "랜덤",
        .bookmarks: "북마크함",
    ]
}

struct PostSourceIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "글 노출 방식"
    static var description = IntentDescription("위젯에 표시할 글을 랜덤으로 고를지, 북마크함에서 고를지 선택하세요.")

    @Parameter(title: "노출 방식", default: .random)
    var source: WidgetPostSource
}

struct PostEntry: TimelineEntry {
    let date: Date
    let post: Post?
}

struct Provider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> PostEntry {
        PostEntry(
            date: .now,
            post: Post(id: "0", title: "pptnz.net", link: "", date: "", content: "새 글을 불러오는 중…")
        )
    }

    func snapshot(for configuration: PostSourceIntent, in context: Context) async -> PostEntry {
        let posts = PostsRepository.shared.loadCachedPosts()
        return PostEntry(date: .now, post: pickPost(from: posts, source: configuration.source))
    }

    func timeline(for configuration: PostSourceIntent, in context: Context) async -> Timeline<PostEntry> {
        let posts = (try? await PostsRepository.shared.loadPosts()) ?? PostsRepository.shared.loadCachedPosts()
        let entry = PostEntry(date: .now, post: pickPost(from: posts, source: configuration.source))
        // 4시간마다 다른 글을 랜덤으로 뽑아서 보여주도록 갱신 주기를 잡는다.
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 4, to: .now) ?? .now
        return Timeline(entries: [entry], policy: .after(nextUpdate))
    }

    private func pickPost(from posts: [Post], source: WidgetPostSource) -> Post? {
        switch source {
        case .random:
            return posts.randomElement()
        case .bookmarks:
            let bookmarked = posts.filter { BookmarksStore.shared.isBookmarked($0) }
            // 북마크한 글이 없으면 빈 위젯 대신 랜덤 글로 대체해서 보여준다.
            return bookmarked.randomElement() ?? posts.randomElement()
        }
    }
}

struct PPTNZBlogWidgetEntryView: View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let post = entry.post {
            VStack(alignment: .leading, spacing: 4) {
                Text("pptnz.net")
                    .font(.caption2.bold())
                    .foregroundStyle(Color.pptnzTeal)
                Text(post.title)
                    .font(.headline)
                    .foregroundStyle(Color.pptnzInk)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                // 사진이 있는 글이어도 위젯에서는 텍스트만 보여준다(이미지는 표시 안 함).
                if !post.content.isEmpty {
                    Text(post.content)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(contentLineLimit)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
                Text(post.date)
                    .font(.caption2)
                    .foregroundStyle(Color.pptnzCoral)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .widgetURL(URL(string: "pptnzblog://post/\(post.id)"))
        } else {
            VStack(alignment: .leading, spacing: 6) {
                Image(systemName: "text.bubble")
                Text("글을 불러오지 못했어요")
                    .font(.caption)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
        }
    }

    private var contentLineLimit: Int {
        switch family {
        case .systemSmall: return 4
        case .systemMedium: return 5
        default: return 10
        }
    }
}

struct PPTNZBlogWidget: Widget {
    let kind = PostsRepository.widgetKind

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: PostSourceIntent.self, provider: Provider()) { entry in
            PPTNZBlogWidgetEntryView(entry: entry)
                .containerBackground(Color.pptnzBackground, for: .widget)
        }
        .configurationDisplayName("pptnz.net")
        .description("페퍼톤스 블로그 글을 보여줍니다. 랜덤 또는 북마크함 중에서 고를 수 있어요.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

#Preview(as: .systemMedium) {
    PPTNZBlogWidget()
} timeline: {
    PostEntry(date: .now, post: .preview)
}
