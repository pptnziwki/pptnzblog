import SwiftUI

/// 태그 필터 칩. 주요 태그 5개와 "기타"(그 외 태그), 그리고 "전체".
enum NewsFilter: String, CaseIterable, Identifiable {
    case all, live, release, tv, radio, work, other

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all: return "전체"
        case .other: return "기타"
        default: return rawValue.uppercased()
        }
    }

    func matches(_ item: NewsItem) -> Bool {
        switch self {
        case .all: return true
        case .other: return !item.tags.contains { Self.primaryTags.contains($0) }
        default: return item.tags.contains(rawValue)
        }
    }

    private static let primaryTags: Set<String> = ["live", "release", "tv", "radio", "work"]
}

/// peppertones.net news 탭 내용을 연도별 토글 목록으로 보여준다.
/// 검색어는 상위(ContentView)의 `.searchable`에서 내려받는다.
struct NewsView: View {
    let searchText: String

    @State private var items: [NewsItem] = []
    @State private var isLoading = false
    @State private var loadError: String?
    @State private var expandedYears: Set<Int> = []
    @State private var didSetInitialExpansion = false  // 처음 한 번만 전체 펼침 상태로 시작
    @State private var filter: NewsFilter = .all
    @Environment(\.openURL) private var openURL

    private var isSearching: Bool { !searchText.isEmpty }

    /// 검색이나 태그 필터가 걸려 있으면 접힌 연도에 있는 결과도 보이도록 항상 펼친다.
    private var forcesExpansion: Bool { isSearching || filter != .all }

    private var filteredItems: [NewsItem] {
        let tagFiltered = items.filter { filter.matches($0) }
        guard isSearching else { return tagFiltered }
        return tagFiltered.filter {
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
        content
        .safeAreaInset(edge: .top, spacing: 0) {
            if !items.isEmpty {
                filterBar
            }
        }
        .background(Color.pptnzBackground)
        .refreshable { await load() }
        .task { await load() }
    }

    /// 상단 태그 필터. 한 번에 하나만 선택한다.
    private var filterBar: some View {
        // 한 줄에 다 들어가면 그대로, 좁은 기기에서는 가로 스크롤로 넘어간다.
        ViewThatFits(in: .horizontal) {
            chipRow
            ScrollView(.horizontal, showsIndicators: false) { chipRow }
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        // 목록이 필터/헤더 뒤로 스크롤돼도 비쳐 보이지 않도록, 상단 네비게이션 영역까지 배경색으로 꽉 채운다.
        .background(Color.pptnzBackground.ignoresSafeArea(edges: .top))
    }

    private var chipRow: some View {
        HStack(spacing: 4) {
            ForEach(NewsFilter.allCases) { option in
                Button {
                    withAnimation { filter = option }
                } label: {
                    Text(option.label)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(filter == option ? Color.pptnzInk : Color.secondary)
                        .lineLimit(1)
                        .fixedSize()
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(Capsule().fill(filter == option ? Color.pptnzSky : Color.secondary.opacity(0.12)))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var content: some View {
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
                    } else if filter != .all {
                        ContentUnavailableView("\(filter.label) 소식이 없어요", systemImage: "line.3.horizontal.decrease.circle")
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
    }

    private var list: some View {
        List {
            Color.clear.frame(height: 0)
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
                .id(Self.topAnchor)
            ForEach(groupedByYear, id: \.year) { group in
                Section {
                    if forcesExpansion || expandedYears.contains(group.year) {
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
                            Image(systemName: forcesExpansion || expandedYears.contains(group.year) ? "chevron.down" : "chevron.right")
                                .foregroundStyle(Color.pptnzPink)
                        }
                        .padding(.vertical, 6)
                        .contentShape(Rectangle())
                        // 스크롤하면 헤더가 상단에 붙는데, 배경이 없으면 아래 행과 글자가 겹쳐 보인다.
                        .background(Color.pptnzBackground)
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
        // 필터 바와 (상단에 고정되는) 연도 헤더 사이의 틈으로 행이 비쳐 보이지 않게 배경색으로 덮는다.
        .overlay(alignment: .top) {
            Color.pptnzBackground
                .frame(height: 20)
                .allowsHitTesting(false)
        }
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

        // 처음 열었을 때는 모든 연도를 펼쳐둔다. (블로그와 같은 기본 상태)
        if !didSetInitialExpansion {
            expandedYears = Set(items.map(\.year))
            didSetInitialExpansion = true
        }
        // 사용자가 NEWS 탭에서 본 소식은 이후 백그라운드 새로고침이 "새 소식"으로 알리지 않게 기록한다.
        NewsNotifier.markSeen(items)
    }
}

struct NewsRowView: View {
    let item: NewsItem
    let onTapLink: () -> Void
    @ObservedObject private var attended = AttendedStore.shared

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            rowContent
                .onTapGesture {
                    if item.linkURL != nil { onTapLink() }
                }
            // 다녀온 공연 체크는 공연(live) 소식에만 붙는다.
            if item.isLive {
                attendedCheckbox
            }
        }
        .listRowBackground(Color.pptnzBackground)
    }

    private var attendedCheckbox: some View {
        let isAttended = attended.isAttended(item)
        return Button {
            attended.toggle(item)
        } label: {
            Image(systemName: isAttended ? "checkmark.square.fill" : "square")
                .font(.title3)
                .foregroundStyle(isAttended ? Color.pptnzCoral : Color.secondary)
        }
        .buttonStyle(.borderless)
        .accessibilityLabel("다녀온 공연")
        .accessibilityValue(isAttended ? "선택됨" : "선택 안 됨")
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
