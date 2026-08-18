import SwiftUI

/// `PostDetail.blocks`의 텍스트 블록을 문단 단위로 펼쳐서 보여주는 헬퍼.
enum ShareParagraphExtractor {
    static func paragraphs(from blocks: [PostContentBlock]) -> [String] {
        blocks.flatMap { block -> [String] in
            guard case .text(let text) = block else { return [] }
            return text
                .components(separatedBy: "\n\n")
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
        }
    }
}

/// 스토리 카드에 넣을 본문 구간을 문단 단위로 고르는 시트.
/// 스포티파이 가사 공유처럼, 문단을 탭하면 시작점이 되고 다른 문단을 이어서 탭하면
/// 그 사이 구간 전체가 선택된다. 선택된 문단을 다시 탭하면 선택이 풀린다.
struct ShareTextPickerView: View {
    let paragraphs: [String]
    let onConfirm: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var anchorIndex: Int?
    @State private var selectedRange: ClosedRange<Int>?

    private static let cardTextLimit = 300

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 10) {
                    ForEach(Array(paragraphs.enumerated()), id: \.offset) { index, paragraph in
                        Text(paragraph)
                            .font(.subheadline)
                            .foregroundStyle(Color.pptnzInk)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(isSelected(index) ? Color.pptnzPink.opacity(0.35) : Color.pptnzPink.opacity(0.08))
                            )
                            .onTapGesture { select(index) }
                    }
                }
                .padding()
            }
            .background(Color.pptnzBackground)
            .navigationTitle("구간 선택")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("카드 생성") { confirm() }
                        .disabled(selectedRange == nil)
                }
            }
        }
    }

    private func isSelected(_ index: Int) -> Bool {
        selectedRange?.contains(index) ?? false
    }

    private func select(_ index: Int) {
        guard let anchor = anchorIndex else {
            anchorIndex = index
            selectedRange = index...index
            return
        }

        if anchor == index, selectedRange == index...index {
            // 선택된 단일 문단을 다시 탭하면 해제.
            anchorIndex = nil
            selectedRange = nil
            return
        }

        let lower = min(anchor, index)
        let upper = max(anchor, index)
        selectedRange = lower...upper
        anchorIndex = index
    }

    private func confirm() {
        guard let selectedRange else { return }
        var text = paragraphs[selectedRange].joined(separator: "\n\n")
        if text.count > Self.cardTextLimit {
            let cutoff = text.index(text.startIndex, offsetBy: Self.cardTextLimit)
            text = String(text[..<cutoff]) + "…"
        }
        onConfirm(text)
        dismiss()
    }
}
