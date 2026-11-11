import SwiftUI

/// peppertones.net news 탭 내용을 연도별 토글 목록으로 보여준다.
/// 검색어는 상위(ContentView)의 `.searchable`에서 내려받는다.
struct NewsView: View {
    let searchText: String

    @State private var items: [NewsItem] = []
    @State private var isLoading = false
    @State private var loadError: String?
    @State private var expandedYears: Set<Int> = []
    @State private var didSetInitialExpansion = false
    @Environment(\.openURL) private var openURL

    private var isSearching: Bool { !searchText.isEmpty }

    private var filteredItems: [NewsItem] {
        guard isSearching else { return items }
        return items.filter {
            $0.content.localizedCaseInsensitiveContains(searchText)
                || $0.tag.localizedCaseInsensitiveContains(searchText)
                || $0.date.localizedCaseInsensitiveContains(searchText)
        }
    }

    /// 연도 내림차순, 각 연도 안에서는 날짜 최신순(날짜가 같으면 원문 순서 유지).
    private var groupedByYear: [(year: Int, items: [NewsItem])] {
        let groups = Dictionary(grouping: filteredItems, by: \.year)
        return groups
            .sorted { $0.key > $1.key }
            .map { (year: $0.key, items: $0.value.enumerated()
                .sorted { ($0.element.sortKey, $1.offset) > ($1.element.sortKey, $0.offset) }
                .map(\.element)) }
    }

    var body: some View {
        ScrollViewReader { proxy in
            Group {
                if items.isEmpty && isLoading {
                    ProgressView("불러오는 중…")
                } else if let loadError, items.isEmpty {
                    ContentUnavailableView(
                        "소식을 불러오지 못했어요",
                        systemImage: "wifi.slash",
                        description: Text(loadError)
                    )
                } else if filteredItems.isEmpty {
                    if isSearching {
                        ContentUnavailableView.search(text: searchText)
                    } else {
                        ContentUnavailableView("아직 소식이 없어요", systemImage: "newspaper")
                    }
                } else {
                    ZStack(alignment: .bottomTrailing) {
                        list
                        scrollButtons(proxy: proxy)
                    }
                }
            }
        }
        .background(Color.pptnzBackground)
        .refreshable { await load() }
        .task { await load() }
    }

    private var list: some View {
        List {
            Color.clear.frame(height: 0)
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
                .id(Self.topAnchor)
            ForEach(groupedByYear, id: \.year) { group in
                Section {
                    // 검색 중에는 접힌 연도에 있는 결과도 보이도록 항상 펼친다.
                    if isSearching || expandedYears.contains(group.year) {
                        ForEach(group.items) { item in
                            NewsRowView(item: item) {
                                if let url = item.linkURL { openURL(url) }
                            }
                        }
                    }
                } header: {
                    Button {
                        withAnimation { toggleYear(group.year) }
                    } label: {
                        HStack {
                            // Int를 LocalizedStringKey로 보간하면 "2,026"처럼 천 단위 구분이 붙으므로 String으로 만든다.
                            Text(String(group.year) + "년")
                                .foregroundStyle(Color.pptnzPink)
                            Spacer()
                            Image(systemName: isSearching || expandedYears.contains(group.year) ? "chevron.down" : "chevron.right")
                                .foregroundStyle(Color.pptnzPink)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            Color.clear.frame(height: 0)
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
                .id(Self.bottomAnchor)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(Color.pptnzBackground)
        .contentMargins(.top, 0, for: .scrollContent)
    }

    private func toggleYear(_ year: Int) {
        if expandedYears.contains(year) {
            expandedYears.remove(year)
        } else {
            expandedYears.insert(year)
        }
    }

    private static let topAnchor = "newsTop"
    private static let bottomAnchor = "newsBottom"

    private func scrollButtons(proxy: ScrollViewProxy) -> some View {
        VStack(spacing: 12) {
            scrollButton(systemImage: "arrow.up") {
                withAnimation { proxy.scrollTo(Self.topAnchor, anchor: .top) }
            }
            scrollButton(systemImage: "arrow.down") {
                withAnimation { proxy.scrollTo(Self.bottomAnchor, anchor: .bottom) }
            }
        }
        .padding(.trailing, 16)
        .padding(.bottom, 16)
    }

    private func scrollButton(systemImage: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.headline)
                .foregroundStyle(Color.pptnzCoral)
                .frame(width: 44, height: 44)
                .background(Circle().fill(Color.white))
                .shadow(color: .black.opacity(0.15), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }

    private func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            items = try await NewsRepository.shared.loadNews()
            loadError = nil
        } catch {
            loadError = error.localizedDescription
            if items.isEmpty {
                items = NewsRepository.shared.loadCachedNews()
            }
        }
        guard !items.isEmpty else { return }

        // 처음 열었을 때는 최신 연도만 펼쳐둔다. (소식이 수백 건이라 전부 펼치면 너무 길다)
        if !didSetInitialExpansion, let latestYear = items.map(\.year).max() {
            expandedYears = [latestYear]
            didSetInitialExpansion = true
        }
        // 사용자가 NEWS 탭에서 본 소식은 이후 백그라운드 새로고침이 "새 소식"으로 알리지 않게 기록한다.
        NewsNotifier.markSeen(items)
    }
}

struct NewsRowView: View {
    let item: NewsItem
    let onTapLink: () -> Void

    var body: some View {
        if item.linkURL != nil {
            Button(action: onTapLink) { rowContent }
                .buttonStyle(.plain)
                .listRowBackground(Color.pptnzBackground)
        } else {
            rowContent
                .listRowBackground(Color.pptnzBackground)
        }
    }

    private var rowContent: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text(item.tag.uppercased())
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(Capsule().fill(Color.secondary.opacity(0.15)))
                Text(item.date)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer(minLength: 0)
                if item.linkURL != nil {
                    Image(systemName: "link")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Text(item.content)
                .font(.subheadline)
                .foregroundStyle(Color.pptnzInk)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }
}

#Preview {
    NewsView(searchText: "")
}
