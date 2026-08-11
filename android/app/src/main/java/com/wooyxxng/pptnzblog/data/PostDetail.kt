package com.wooyxxng.pptnzblog.data

/** 글 상세 본문을 이루는 조각. 원문 순서를 그대로 유지한다. */
sealed class PostContentBlock {
    data class Text(val text: String) : PostContentBlock()
    data class Image(val url: String) : PostContentBlock()

    /** 구형 Flash `<object>/<embed>` 방식으로 삽입된 유튜브 영상. 값은 유튜브 동영상 ID. */
    data class YouTube(val videoId: String) : PostContentBlock()
}

/** 원문 페이지에서 파싱한 댓글 한 개. */
data class PostComment(
    val author: String,
    val date: String,
    val text: String,
)

/**
 * `PostDetailLoader`가 글 원문(`post.link`)을 실시간으로 가져와 파싱한 결과.
 * posts.json에는 없는 이미지/댓글/첨부 링크까지 포함한다.
 */
data class PostDetail(
    val blocks: List<PostContentBlock>,
    val comments: List<PostComment>,
    val links: List<String>,
) {
    companion object {
        val empty = PostDetail(emptyList(), emptyList(), emptyList())
    }
}
