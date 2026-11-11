import SwiftUI

/// 앱 상단 헤더에서 BLOG / NEWS를 오가는 토글.
enum HomeSection: String, CaseIterable, Identifiable {
    case blog = "BLOG"
    case news = "NEWS"

    var id: String { rawValue }
}

/// iOS 기본 세그먼트 컨트롤. 별도 색을 입히지 않고 시스템 스타일을 그대로 쓴다.
struct SectionToggleView: View {
    @Binding var selection: HomeSection

    var body: some View {
        Picker("섹션", selection: $selection) {
            ForEach(HomeSection.allCases) { section in
                Text(section.rawValue).tag(section)
            }
        }
        .pickerStyle(.segmented)
        .frame(width: 160)
    }
}

#Preview {
    SectionToggleView(selection: .constant(.blog))
}
