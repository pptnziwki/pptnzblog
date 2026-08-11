package com.wooyxxng.pptnzblog.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PPTNZ 블로그 글 한 편.
 * crawl_pptnz_blog.py가 생성하는 posts.json의 각 항목과 1:1로 대응한다.
 */
@Serializable
data class Post(
    val id: String,
    @SerialName("title") private val rawTitle: String,
    val link: String,
    val date: String,
    val content: String,
) {
    /** 제목이 없는 글은 항상 "-"로 표시한다. */
    val title: String
        get() = rawTitle.ifEmpty { "-" }

    /**
     * date 문자열에서 연도(4자리 숫자)를 뽑아낸다.
     * Textcube 날짜 표기 형식 방어적으로, 문자열 안 첫 4자리 숫자를 연도로 간주한다.
     */
    val year: Int?
        get() = Regex("""\d{4}""").find(date)?.value?.toIntOrNull()

    val yearLabel: String
        get() = year?.let { "${it}년" } ?: "연도 미상"

    /** 새 글 감지 시 문자열이 아닌 숫자로 비교하기 위한 편의 프로퍼티. */
    val numericID: Int
        get() = id.toIntOrNull() ?: 0

    companion object {
        val preview = Post(
            id = "1",
            rawTitle = "샘플 제목",
            link = "https://example.com",
            date = "2024.03.15",
            content = "본문 미리보기 텍스트입니다.",
        )
    }
}
