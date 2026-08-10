import WidgetKit
import SwiftUI

struct PostEntry: TimelineEntry {
    let date: Date
    let post: Post?
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> PostEntry {
        PostEntry(
            date: .now,
            post: Post(id: "0", title: "PPTNZ 블로그", link: "", date: "", content: "새 글을 불러오는 중…")
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (PostEntry) -> Void) {
        let cached = PostsRepository.shared.loadCachedPosts()
        completion(PostEntry(date: .now, post: cached.randomElement()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PostEntry>) -> Void) {
        Task {
            let posts = (try? await PostsRepository.shared.loadPosts())
                ?? PostsRepository.shared.loadCachedPosts()
            let entry = PostEntry(date: .now, post: posts.randomElement())
            // 4시간마다 다른 글을 랜덤으로 뽑아서 보여주도록 갱신 주기를 잡는다.
            let nextUpdate = Calendar.current.date(byAdding: .hour, value: 4, to: .now) ?? .now
            completion(Timeline(entries: [entry], policy: .after(nextUpdate)))
        }
    }
}

struct PPTNZBlogWidgetEntryView: View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let post = entry.post {
            VStack(alignment: .leading, spacing: 6) {
                Text("PPTNZ 블로그")
                    .font(.caption2.bold())
                    .foregroundStyle(Color.pptnzTeal)
                if !post.title.isEmpty {
                    Text(post.title)
                        .font(.headline)
                        .foregroundStyle(Color.pptnzInk)
                        .lineLimit(family == .systemSmall ? 3 : 2)
                }
                if !post.content.isEmpty {
                    Text(post.content)
                        .font(post.title.isEmpty ? .subheadline : .caption)
                        .foregroundStyle(post.title.isEmpty ? Color.pptnzInk : .secondary)
                        .lineLimit(family == .systemLarge ? 6 : 3)
                }
                Spacer()
                Text(post.date)
                    .font(.caption2)
                    .foregroundStyle(Color.pptnzCoral)
            }
            .padding()
            .widgetURL(URL(string: post.link))
        } else {
            VStack(spacing: 6) {
                Image(systemName: "text.bubble")
                Text("글을 불러오지 못했어요")
                    .font(.caption)
            }
            .padding()
        }
    }
}

struct PPTNZBlogWidget: Widget {
    let kind = "PPTNZBlogWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            PPTNZBlogWidgetEntryView(entry: entry)
                .containerBackground(Color.pptnzBackground, for: .widget)
        }
        .configurationDisplayName("PPTNZ 블로그")
        .description("페퍼톤스 블로그 글을 랜덤으로 보여줍니다.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

#Preview(as: .systemMedium) {
    PPTNZBlogWidget()
} timeline: {
    PostEntry(date: .now, post: .preview)
}
