import SwiftUI

/// 앱 상단 헤더에서 BLOG / NEWS를 오가는 토글.
enum HomeSection: String, CaseIterable, Identifiable {
    case blog = "BLOG"
    case news = "NEWS"

    var id: String { rawValue }
}

/// 헤더 BLOG / NEWS 토글. 글자색은 하늘색(#ACD9E9), 배경은 유리(Liquid Glass) 느낌.
/// 하늘색 글자가 읽히도록 유리에 어두운 틴트를 살짝 입히고,
/// 선택된 탭은 그 위에 밝은 반투명 알약을 올린다. (iOS 26 미만은 시스템 머티리얼로 대체)
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
                        .foregroundStyle(Color.pptnzSky.opacity(selection == section ? 1 : 0.65))
                        .frame(width: 84, height: 34)
                        .background {
                            if selection == section {
                                Capsule()
                                    .fill(Color.white.opacity(0.22))
                                    .matchedGeometryEffect(id: "pill", in: pill)
                            }
                        }
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == section ? .isSelected : [])
            }
        }
        .padding(3)
        .modifier(GlassCapsule())
    }
}

private struct GlassCapsule: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular.tint(Color.pptnzInk.opacity(0.55)), in: Capsule())
        } else {
            content
                .background(Capsule().fill(Color.pptnzInk.opacity(0.55)))
                .background(.ultraThinMaterial, in: Capsule())
        }
    }
}

#Preview {
    SectionToggleView(selection: .constant(.blog))
}
