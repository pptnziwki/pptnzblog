import Foundation
import SwiftSoup

enum PostDetailError: Error {
    case invalidURL
    case decodingFailed
}

/// 글 상세를 열 때 원문 페이지(`post.link`)를 실시간으로 가져와 파싱한다.
/// posts.json은 목록/새 글 감지용 가벼운 데이터만 담고 있어서,
/// 이미지/댓글/첨부 링크는 크롤러가 아니라 앱이 그때그때 직접 읽어온다.
final class PostDetailLoader {
    static let shared = PostDetailLoader()

    private let baseURL = URL(string: "http://peppertones.host.whoisweb.net")!
    private let userAgent = "PPTNZFanWidget/1.0 (contact: wooyxxng@gmail.com)"

    func load(for post: Post) async throws -> PostDetail {
        guard let url = URL(string: post.link) else { throw PostDetailError.invalidURL }

        var request = URLRequest(url: url)
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")

        let (data, _) = try await URLSession.shared.data(for: request)
        guard let html = String(data: data, encoding: .utf8) else {
            throw PostDetailError.decodingFailed
        }

        let doc = try SwiftSoup.parse(html, baseURL.absoluteString)

        guard let article = try doc.select("div.article").first() else {
            return .empty
        }

        return PostDetail(
            blocks: try parseBlocks(from: article),
            comments: try parseComments(from: doc, postID: post.id),
            links: try parseLinks(from: article)
        )
    }

    // MARK: - 본문 (텍스트 + 이미지, 원문 순서 유지)

    private func parseBlocks(from article: Element) throws -> [PostContentBlock] {
        var blocks: [PostContentBlock] = []
        var buffer = ""
        var seenVideoIDs = Set<String>()

        func flushText() {
            let trimmed = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                blocks.append(.text(trimmed))
            }
            buffer = ""
        }

        for element in try article.getAllElements().array() {
            let tag = element.tagName().lowercased()

            if tag == "img" {
                if let src = try? element.attr("abs:src"), let url = URL(string: src), !src.isEmpty {
                    flushText()
                    blocks.append(.image(url))
                }
                continue
            }

            // 옛 글들은 유튜브 영상을 <iframe>이 아니라 구형 Flash <object>/<embed>로 삽입했다.
            // (예: <object><param name="movie" value="http://www.youtube.com/v/{id}&..."></object>)
            if tag == "object" || tag == "embed" || tag == "iframe" {
                if let videoID = youTubeVideoID(in: element), !seenVideoIDs.contains(videoID) {
                    seenVideoIDs.insert(videoID)
                    flushText()
                    blocks.append(.youtube(videoID))
                }
                continue
            }

            let ownText = element.ownText()
            if !ownText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                buffer += ownText + "\n"
            }
        }
        flushText()
        return blocks
    }

    private func youTubeVideoID(in element: Element) -> String? {
        var candidates: [String] = []
        if let src = try? element.attr("src"), !src.isEmpty {
            candidates.append(src)
        }
        if let movie = try? element.select("param[name=movie]").first()?.attr("value"), !movie.isEmpty {
            candidates.append(movie)
        }
        for candidate in candidates {
            if let id = Self.extractYouTubeID(from: candidate) {
                return id
            }
        }
        return nil
    }

    private static let youTubeIDPattern = try! NSRegularExpression(
        pattern: #"(?:youtube\.com/(?:v|embed)/|youtube\.com/watch\?v=|youtu\.be/)([A-Za-z0-9_-]{6,})"#
    )

    private static func extractYouTubeID(from urlString: String) -> String? {
        let decoded = urlString.replacingOccurrences(of: "&amp;", with: "&")
        let range = NSRange(decoded.startIndex..., in: decoded)
        guard let match = youTubeIDPattern.firstMatch(in: decoded, range: range),
              let idRange = Range(match.range(at: 1), in: decoded) else { return nil }
        return String(decoded[idRange])
    }

    // MARK: - 첨부/참고 링크

    private func parseLinks(from article: Element) throws -> [URL] {
        var urls: [URL] = []
        for a in try article.select("a[href]").array() {
            let href = try a.attr("abs:href")
            guard !href.isEmpty,
                  let url = URL(string: href),
                  url.scheme == "http" || url.scheme == "https" else { continue }
            urls.append(url)
        }
        return urls
    }

    // MARK: - 댓글
    // 실제 마크업: <div class="commentList"><ol><li class="rp_general">...</li>...</ol></div>
    // commentList의 직계 자식은 <ol> 하나뿐이라 거기서 바로 순회하면 댓글이 1개로만 잡힌다.
    // 댓글 각각을 감싸는 .rp_general을 직접 찾아야 전체 댓글이 다 나온다.
    private func parseComments(from doc: Document, postID: String) throws -> [PostComment] {
        guard let commentBox = try doc.select("#entry\(postID)Comment .commentList").first() else {
            return []
        }

        var comments: [PostComment] = []
        for item in try commentBox.select(".rp_general").array() {
            let text = firstText(in: item, selector: "p, .re, .content, .text") ?? (try? item.text()) ?? ""
            let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmedText.isEmpty else { continue }

            let author = firstText(in: item, selector: ".name") ?? "익명"
            let date = firstText(in: item, selector: ".date") ?? ""

            comments.append(PostComment(author: author, date: date, text: trimmedText))
        }
        return comments
    }

    private func firstText(in element: Element, selector: String) -> String? {
        guard let elements = try? element.select(selector),
              let found = elements.first(),
              let text = try? found.text() else { return nil }
        return text
    }
}
