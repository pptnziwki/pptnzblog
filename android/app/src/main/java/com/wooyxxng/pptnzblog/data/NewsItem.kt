package com.wooyxxng.pptnzblog.data

import kotlinx.serialization.Serializable

/**
 * peppertones.net news 탭의 소식 한 건.
 * crawl_pptnz_news.py가 생성하는 news.json의 각 항목과 1:1로 대응한다. (iOS `NewsItem`)
 */
@Serializable
data class NewsItem(
    /** 내용 기반 해시. 새 소식 감지에 쓰이므로 순서와 무관하게 안정적이다. */
    val id: String,
    /** "live", "tv", "release" 등 원문의 `-tag`. 소문자로 정규화되어 있다. */
    val tag: String,
    /** 원문 날짜 표기 그대로. ("2026/11/1" 등) */
    val date: String,
    val year: Int,
    val content: String,
    /** 소식에 달려 있는 첫 번째 링크(예매처 등). 없으면 null. */
    val link: String? = null,
) {
    /** "work & live"처럼 여러 태그가 묶여 있을 수 있어서 개별 태그로 나눈다. */
    val tags: List<String> get() = tag.split("&").map { it.trim() }

    /** 공연(live) 소식인지. 다녀온 공연 체크는 이 소식에만 붙는다. */
    val isLive: Boolean get() = "live" in tags

    val yearLabel: String get() = "${year}년"

    /**
     * 같은 연도 안에서 최신순으로 정렬하기 위한 값 (yyyymmdd).
     * 날짜 문자열 안의 첫 `연/월(/일)`만 읽고, 없는 부분은 0으로 둔다.
     */
    val sortKey: Int
        get() {
            val match = SORT_KEY_REGEX.find(date) ?: return year * 10000
            val month = match.groupValues[2].toIntOrNull() ?: 0
            val day = match.groupValues[3].toIntOrNull() ?: 0
            return year * 10000 + month * 100 + day
        }

    companion object {
        private val SORT_KEY_REGEX = Regex("""(\d{4})/(\d{1,2})(?:/(\d{1,2}))?""")
    }
}
