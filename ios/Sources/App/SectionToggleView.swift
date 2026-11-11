import SwiftUI

/// 앱 상단 헤더에서 BLOG / NEWS를 오가는 토글.
enum HomeSection: String, CaseIterable, Identifiable {
    case blog = "BLOG"
    case news = "NEWS"

    var id: String { rawValue }
}

/// 헤더 BLOG / NEWS 토글. 하늘색(#ACD9E9)을 쓰되 가독성을 위해
/// 선택된 탭은 하늘색 배경 + 진한 글자, 선택 안 된 탭은 더 진한 하늘색 글자로 쓴다.
/// (iOS 26의 시스템 세그먼트 컨트롤은 선택 배경색을 무시해서 직접 그린다.)
struct SectionToggleView: View {
    @Binding var selection: HomeSection
    @Namespace private var pill

    var body: some View {
        HStack(spacing: 0) {
            ForEach(HomeSection.allCases) { section in
                Button {
                    withAnimation(.snappy(duration: 0.2)) { selection = section }
                } label: {
                    Text(section.rawValue)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(selection == section ? Color.pptnzInk : Color.pptnzSkyDeep)
                        .frame(width: 84, height: 34)
                        .background {
                            if selection == section {
                                Capsule()
                                    .fill(Color.pptnzSky)
                                    .matchedGeometryEffect(id: "pill", in: pill)
                            }
                        }
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == section ? .isSelected : [])
            }
        }
    }
}

#Preview {
    SectionToggleView(selection: .constant(.blog))
}
