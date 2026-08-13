import Foundation

/// 앱 업데이트 없이 공지사항을 띄우기 위한 원격 데이터.
/// `id`가 바뀌면 이전에 닫았던 유저에게도 새 공지로 다시 노출된다.
struct Notice: Codable, Equatable {
    let id: String
    let message: String
}

/// notice.json을 GitHub raw URL에서 읽어온다.
/// 실패해도 앱 핵심 기능(글 목록)에는 영향이 없어야 하므로 에러는 조용히 무시하고 nil을 반환한다.
enum NoticeRepository {
    private static let remoteURL = URL(
        string: "https://raw.githubusercontent.com/wooyxxng-Jang/pptnzblog/main/notice.json"
    )!

    static func loadNotice() async -> Notice? {
        guard let (data, _) = try? await URLSession.shared.data(from: remoteURL),
              let notice = try? JSONDecoder().decode(Notice.self, from: data),
              !notice.message.isEmpty
        else { return nil }
        return notice
    }
}
