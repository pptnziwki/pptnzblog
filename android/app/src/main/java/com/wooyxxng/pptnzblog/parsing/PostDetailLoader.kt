package com.wooyxxng.pptnzblog.parsing

import com.wooyxxng.pptnzblog.data.Post
import com.wooyxxng.pptnzblog.data.PostComment
import com.wooyxxng.pptnzblog.data.PostContentBlock
import com.wooyxxng.pptnzblog.data.PostDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

/**
 * 글 상세를 열 때 원문 페이지(`post.link`)를 실시간으로 가져와 파싱한다.
 * posts.json은 목록/새 글 감지용 가벼운 데이터만 담고 있어서,
 * 이미지/댓글/첨부 링크는 크롤러가 아니라 앱이 그때그때 직접 읽어온다.
 *
 * SwiftSoup(iOS)의 원본 JVM 라이브러리인 Jsoup으로 동일한 파싱 로직을 그대로 이식했다.
 */
class PostDetailLoader private constructor() {
    private val client = OkHttpClient()
    private val baseUrl = "http://peppertones.host.whoisweb.net"
    private val userAgent = "PPTNZFanWidget/1.0 (contact: wooyxxng@gmail.com)"

    suspend fun load(post: Post): PostDetail = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(post.link)
            .header("User-Agent", userAgent)
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("empty body")
        }

        val doc = Jsoup.parse(html, baseUrl)
        val article = doc.select("div.article").firstOrNull() ?: return@withContext PostDetail.empty

        PostDetail(
            blocks = parseBlocks(article),
            comments = parseComments(doc, post.id),
            links = parseLinks(article),
        )
    }

    // MARK: - 본문 (텍스트 + 이미지, 원문 순서 유지)

    private fun parseBlocks(article: Element): List<PostContentBlock> {
        val blocks = mutableListOf<PostContentBlock>()
        val paragraphs = mutableListOf<String>()
        var currentParagraph = StringBuilder()
        val seenVideoIds = mutableSetOf<String>()

        // <P>/<DIV> 자체는 텍스트가 없고 그 안의 <FONT>/<A>/<B> 같은 인라인 태그가
        // 실제 텍스트를 담고 있는 경우가 많다. 모든 요소마다 무조건 줄바꿈을 넣으면
        // 문단 안 인라인 태그 때문에 문장이 뚝뚝 끊겨 나오므로, <BR>은 줄바꿈 한 번,
        // <P>/<DIV>는 문단 구분(빈 줄)으로만 처리하고 나머지는 이어 붙인다.
        fun appendOwnText(text: String) {
            if (text.isEmpty()) return
            val last = currentParagraph.lastOrNull()
            if (last != null && !last.isWhitespace()) {
                currentParagraph.append(' ')
            }
            currentParagraph.append(text)
        }

        fun flushParagraph() {
            val trimmed = currentParagraph.toString().trim()
            if (trimmed.isNotEmpty()) {
                paragraphs.add(trimmed)
            }
            currentParagraph = StringBuilder()
        }

        fun flushText() {
            flushParagraph()
            if (paragraphs.isNotEmpty()) {
                blocks.add(PostContentBlock.Text(paragraphs.joinToString("\n\n")))
                paragraphs.clear()
            }
        }

        for (element in article.allElements) {
            val tag = element.tagName().lowercase()

            if (tag == "img") {
                val src = element.attr("abs:src")
                if (src.isNotEmpty()) {
                    flushText()
                    blocks.add(PostContentBlock.Image(src))
                }
                continue
            }

            // 옛 글들은 유튜브 영상을 <iframe>이 아니라 구형 Flash <object>/<embed>로 삽입했다.
            if (tag == "object" || tag == "embed" || tag == "iframe") {
                val videoId = youTubeVideoId(element)
                if (videoId != null && seenVideoIds.add(videoId)) {
                    flushText()
                    blocks.add(PostContentBlock.YouTube(videoId))
                }
                continue
            }

            // 실제로는 위 <object>/<embed> 마크업이 진짜 DOM에 있는 게 아니라,
            // <script>writeCode2("<object>...</object>")</script> 처럼 JS 문자열 안에만 있는
            // 경우가 대부분이다. Jsoup은 JS를 실행하지 않으므로 그 태그들이 실제 Element로
            // 잡히지 않는다. 그래서 <script> 원문 텍스트에서 직접 유튜브 링크를 찾는다.
            if (tag == "script") {
                val videoId = extractYouTubeId(element.data())
                if (videoId != null && seenVideoIds.add(videoId)) {
                    flushText()
                    blocks.add(PostContentBlock.YouTube(videoId))
                }
                continue
            }

            if (tag == "br") {
                currentParagraph.append('\n')
                continue
            }

            if (tag == "p" || tag == "div") {
                // <P>/<DIV>는 문단 경계지만, <FONT> 같은 감싸는 태그 없이
                // 텍스트가 바로 그 안에 들어있는 경우도 있다. continue로 건너뛰면
                // 그 텍스트가 통째로 사라지므로, 이전 문단을 flush한 뒤 이 요소의
                // own text도 새 문단에 이어붙여야 한다.
                flushParagraph()
                appendOwnText(element.ownText())
                continue
            }

            appendOwnText(element.ownText())
        }
        flushText()
        return blocks
    }

    private fun youTubeVideoId(element: Element): String? {
        val candidates = mutableListOf<String>()
        val src = element.attr("src")
        if (src.isNotEmpty()) candidates.add(src)
        val movie = element.select("param[name=movie]").firstOrNull()?.attr("value")
        if (!movie.isNullOrEmpty()) candidates.add(movie)
        for (candidate in candidates) {
            extractYouTubeId(candidate)?.let { return it }
        }
        return null
    }

    private val youTubeIdPattern = Regex(
        """(?:youtube\.com/(?:v|embed)/|youtube\.com/watch\?v=|youtu\.be/)([A-Za-z0-9_-]{6,})"""
    )

    private fun extractYouTubeId(urlString: String): String? {
        val decoded = urlString.replace("&amp;", "&")
        return youTubeIdPattern.find(decoded)?.groupValues?.get(1)
    }

    // MARK: - 첨부/참고 링크

    private fun parseLinks(article: Element): List<String> {
        val urls = mutableListOf<String>()
        for (a in article.select("a[href]")) {
            val href = a.attr("abs:href")
            if (href.isEmpty()) continue
            if (href.startsWith("http://") || href.startsWith("https://")) {
                urls.add(href)
            }
        }
        return urls
    }

    // MARK: - 댓글
    // 실제 마크업: <div class="commentList"><ol><li class="rp_general">...</li>...</ol></div>
    // commentList의 직계 자식은 <ol> 하나뿐이라 거기서 바로 순회하면 댓글이 1개로만 잡힌다.
    // 댓글 각각을 감싸는 .rp_general을 직접 찾아야 전체 댓글이 다 나온다.
    private fun parseComments(doc: Document, postId: String): List<PostComment> {
        val commentBox = doc.select("#entry${postId}Comment .commentList").firstOrNull()
            ?: return emptyList()

        val comments = mutableListOf<PostComment>()
        for (item in commentBox.select(".rp_general")) {
            val text = firstText(item, "p, .re, .content, .text") ?: item.text()
            val trimmedText = text.trim()
            if (trimmedText.isEmpty()) continue

            val author = firstText(item, ".name") ?: "익명"
            val date = firstText(item, ".date") ?: ""

            comments.add(PostComment(author = author, date = date, text = trimmedText))
        }
        return comments
    }

    private fun firstText(element: Element, selector: String): String? {
        return element.select(selector).firstOrNull()?.text()
    }

    companion object {
        val shared = PostDetailLoader()
    }
}
