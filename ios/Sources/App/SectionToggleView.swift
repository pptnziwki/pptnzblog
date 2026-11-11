import SwiftUI

/// 앱 상단 헤더에서 BLOG / NEWS를 오가는 토글.
enum HomeSection: String, CaseIterable, Identifiable {
    case blog = "BLOG"
    case news = "NEWS"

    var id: String { rawValue }
}

struct SectionToggleView: View {
    @Binding var selection: HomeSection

    var body: some View {
        HStack(spacing: 2) {
            ForEach(HomeSection.allCases) { section in
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { selection = section }
                } label: {
                    Text(section.rawValue)
                        .font(.system(.title3, design: .default, weight: .heavy))
                        .foregroundStyle(selection == section ? Color.pptnzYellow : Color.pptnzCoral.opacity(0.55))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background {
                            if selection == section {
                                Capsule().fill(Color.pptnzCoral)
                            }
                        }
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == section ? .isSelected : [])
            }
        }
        .padding(2)
        .background(Capsule().fill(Color.pptnzCoral.opacity(0.12)))
    }
}

#Preview {
    SectionToggleView(selection: .constant(.blog))
}
