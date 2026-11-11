import SwiftUI
import UIKit

/// 앱 상단 헤더에서 BLOG / NEWS를 오가는 토글.
enum HomeSection: String, CaseIterable, Identifiable {
    case blog = "BLOG"
    case news = "NEWS"

    var id: String { rawValue }
}

/// iOS 기본 세그먼트 컨트롤 그대로, 글자만 크게·볼드·하늘색(#92C9DE)으로 바꾼 토글.
/// 배경/틴트는 건드리지 않아서 시스템 스타일(iOS 26에서는 유리)을 그대로 따른다.
struct SectionToggleView: View {
    @Binding var selection: HomeSection

    init(selection: Binding<HomeSection>) {
        _selection = selection
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 18, weight: .bold),
            .foregroundColor: UIColor(Color.pptnzSkyText),
        ]
        let appearance = UISegmentedControl.appearance()
        appearance.setTitleTextAttributes(attributes, for: .normal)
        appearance.setTitleTextAttributes(attributes, for: .selected)
    }

    var body: some View {
        Picker("섹션", selection: $selection) {
            ForEach(HomeSection.allCases) { section in
                Text(section.rawValue).tag(section)
            }
        }
        .pickerStyle(.segmented)
        .frame(width: 200)
    }
}

#Preview {
    SectionToggleView(selection: .constant(.blog))
}
