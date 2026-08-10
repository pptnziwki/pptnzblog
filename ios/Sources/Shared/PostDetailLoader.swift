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

        func flushText() {
            let trimmed = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                blocks.append(.text(trimmed))
            }
            buffer = ""
        }

        for element in try article.getAllElements().array() {
            if element.tagName().lowercased() == "img" {
                if let src = try? element.attr("abs:src"), let url = URL(string: src), !src.isEmpty {
                    flushText()
                    blocks.append(.image(url))
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
    // Textcube 댓글 마크업이 정확히 어떤 모양인지(실제 댓글이 달린 글을 못 봐서) 확정되지 않아,
    // 여러 형태를 방어적으로 시도한다. 못 찾으면 빈 배열을 돌려주고 화면에서 "댓글 없음"으로 처리한다.
    private func parseComments(from doc: Document, postID: String) throws -> [PostComment] {
        guard let commentBox = try doc.select("#entry\(postID)Comment .commentList").first() else {
            return []
        }

        var comments: [PostComment] = []
        for item in commentBox.children().array() {
            let text = firstText(in: item, selector: ".re, .content, .text, p") ?? (try? item.text()) ?? ""
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
