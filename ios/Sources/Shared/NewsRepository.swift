import Foundation

/// news.json을 GitHub raw URL에서 내려받아 App Group 공유 컨테이너에 캐시한다.
/// PostsRepository와 같은 방식(네트워크 우선, 실패하면 캐시)으로 동작한다.
final class NewsRepository {
    static let shared = NewsRepository()

    /// crawl.yml이 news.json을 main 브랜치에 커밋하므로 그걸 그대로 읽는다.
    private let remoteURL = URL(
        string: "https://raw.githubusercontent.com/wooyxxng-Jang/pptnzblog/main/news.json"
    )!

    private var cacheFileURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: PostsRepository.appGroupID)?
            .appendingPathComponent("news_cache.json")
    }

    /// 네트워크 우선, 실패하면 캐시로 폴백. 캐시도 없으면 에러를 던진다.
    func loadNews() async throws -> [NewsItem] {
        do {
            let (data, response) = try await URLSession.shared.data(from: remoteURL)
            if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                throw URLError(.badServerResponse)
            }
            let items = try JSONDecoder().decode([NewsItem].self, from: data)
            writeCache(data)
            return items
        } catch {
            if let cached = readCache() {
                return cached
            }
            throw error
        }
    }

    func loadCachedNews() -> [NewsItem] {
        readCache() ?? []
    }

    private func readCache() -> [NewsItem]? {
        guard let url = cacheFileURL,
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode([NewsItem].self, from: data)
    }

    private func writeCache(_ data: Data) {
        guard let url = cacheFileURL else { return }
        try? data.write(to: url, options: .atomic)
    }
}
