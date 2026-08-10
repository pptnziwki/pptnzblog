import Foundation

/// 글 상세 본문을 이루는 조각. 원문 순서를 그대로 유지한다.
enum PostContentBlock: Hashable {
    case text(String)
    case image(URL)
    /// 구형 Flash `<object>/<embed>` 방식으로 삽입된 유튜브 영상. 값은 유튜브 동영상 ID.
    case youtube(String)
}

/// 원문 페이지에서 파싱한 댓글 한 개.
struct PostComment: Identifiable, Hashable {
    let id = UUID()
    let author: String
    let date: String
    let text: String
}

/// `PostDetailLoader`가 글 원문(`post.link`)을 실시간으로 가져와 파싱한 결과.
/// posts.json에는 없는 이미지/댓글/첨부 링크까지 포함한다.
struct PostDetail {
    let blocks: [PostContentBlock]
    let comments: [PostComment]
    /// 본문 안의 외부 링크(첨부/참고 링크 등).
    let links: [URL]

    static let empty = PostDetail(blocks: [], comments: [], links: [])
}
