import SwiftUI

/// notice.json 기반 공지 배너. 닫으면 같은 id의 공지는 다시 뜨지 않는다.
struct NoticeBannerView: View {
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "megaphone.fill")
                .foregroundStyle(Color.pptnzCoral)
            Text(message)
                .font(.footnote)
                .foregroundStyle(Color.pptnzInk)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.caption)
                    .foregroundStyle(Color.pptnzInk.opacity(0.5))
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(Color.pptnzPink.opacity(0.25))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }
}
