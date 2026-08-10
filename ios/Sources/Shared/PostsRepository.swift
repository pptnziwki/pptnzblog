import Foundation

/// posts.json을 GitHub raw URL에서 내려받아 App Group 공유 컨테이너에 캐시한다.
/// 앱과 위젯 익스텐션이 동일한 캐시를 읽고 쓰기 때문에, 둘 중 하나가 최근에
/// 갱신했다면 나머지는 네트워크 없이 그 결과를 바로 쓸 수 있다.
final class PostsRepository {
    static let shared = PostsRepository()

    /// Xcode "Signing & Capabilities > App Groups"에서 앱/위젯 두 타겟 모두에
    /// 등록한 값과 반드시 동일해야 한다. (기본값은 project.yml의 entitlements와 일치)
    static let appGroupID = "group.com.wooyxxng.pptnzblog"

    /// WidgetKit이 타임라인을 갱신할 때 쓰는 위젯 kind. 위젯 정의와 문자열이 반드시 일치해야 한다.
    static let widgetKind = "PPTNZBlogWidget"

    /// crawl.yml이 posts.json을 main 브랜치에 커밋하므로 그걸 그대로 읽는다.
    private let remoteURL = URL(
        string: "https://raw.githubusercontent.com/wooyxxng-Jang/pptnzblog/main/posts.json"
    )!

    private var cacheFileURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: Self.appGroupID)?
            .appendingPathComponent("posts_cache.json")
    }

    /// 네트워크 우선, 실패하면 캐시로 폴백. 캐시도 없으면 에러를 던진다.
    func loadPosts() async throws -> [Post] {
        do {
            let (data, _) = try await URLSession.shared.data(from: remoteURL)
            let posts = try JSONDecoder().decode([Post].self, from: data)
            writeCache(data)
            return posts
        } catch {
            if let cached = readCache() {
                return cached
            }
            throw error
        }
    }

    /// 네트워크 왕복 없이 즉시 뭔가 보여줘야 하는 위젯 placeholder/snapshot용.
    func loadCachedPosts() -> [Post] {
        readCache() ?? []
    }

    private func readCache() -> [Post]? {
        guard let url = cacheFileURL,
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode([Post].self, from: data)
    }

    private func writeCache(_ data: Data) {
        guard let url = cacheFileURL else { return }
        try? data.write(to: url, options: .atomic)
    }
}
