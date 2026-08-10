import SwiftUI

struct ContentView: View {
    @State private var posts: [Post] = []
    @State private var searchText = ""
    @State private var isLoading = false
    @State private var loadError: String?
    @State private var showsSettings = false
    @State private var showsBookmarks = false
    @State private var navigationPath = NavigationPath()
    @ObservedObject private var bookmarks = BookmarksStore.shared
    @ObservedObject private var notificationDelegate = NotificationDelegate.shared

    private var filteredPosts: [Post] {
        guard !searchText.isEmpty else { return posts }
        return posts.filter {
            $0.title.localizedCaseInsensitiveContains(searchText)
                || $0.content.localizedCaseInsensitiveContains(searchText)
        }
    }

    /// 연도 내림차순, 각 연도 안에서는 글 id(Textcube 발행 순서) 내림차순.
    private var groupedByYear: [(year: String, posts: [Post])] {
        let groups = Dictionary(grouping: filteredPosts, by: \.yearLabel)
        return groups
            .sorted { $0.key > $1.key }
            .map { (year: $0.key, posts: $0.value.sorted { $0.id > $1.id }) }
    }

    private var bookmarkedPosts: [Post] {
        posts.filter { bookmarks.isBookmarked($0) }.sorted { $0.id > $1.id }
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ScrollViewReader { proxy in
                Group {
                    if posts.isEmpty && isLoading {
                        ProgressView("불러오는 중…")
                    } else if let loadError, posts.isEmpty {
                        ContentUnavailableView(
                            "글을 불러오지 못했어요",
                            systemImage: "wifi.slash",
                            description: Text(loadError)
                        )
                    } else if filteredPosts.isEmpty {
                        ContentUnavailableView.search(text: searchText)
                    } else {
                        ZStack(alignment: .bottomTrailing) {
                            List {
                                Color.clear.frame(height: 0).id(Self.topAnchor)
                                ForEach(groupedByYear, id: \.year) { group in
                                    Section {
                                        ForEach(group.posts) { post in
                                            NavigationLink(value: post) {
                                                PostRowView(post: post)
                                            }
                                        }
                                    } header: {
                                        Text(group.year)
                                            .foregroundStyle(Color.pptnzPink)
                                    }
                                }
                                Color.clear.frame(height: 0).id(Self.bottomAnchor)
                            }
                            .listStyle(.insetGrouped)
                            .scrollContentBackground(.hidden)
                            .background(Color.pptnzBackground)

                            scrollButtons(proxy: proxy)
                        }
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        showsBookmarks = true
                    } label: {
                        Image(systemName: "bookmark")
                            .foregroundStyle(Color.pptnzCoral)
                    }
                    .buttonStyle(.plain)
                }
                ToolbarItem(placement: .principal) {
                    Text("BLOG")
                        .font(.system(.title2, design: .default, weight: .heavy))
                        .foregroundStyle(Color.pptnzYellow)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showsSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                            .foregroundStyle(Color.pptnzCoral)
                    }
                    .buttonStyle(.plain)
                }
            }
            .tint(Color.pptnzCoral)
            .searchable(text: $searchText, prompt: "제목·본문 검색")
            .navigationDestination(for: Post.self) { post in
                PostDetailView(post: post)
            }
            .sheet(isPresented: $showsSettings) {
                SettingsView()
            }
            .sheet(isPresented: $showsBookmarks) {
                BookmarksView(posts: bookmarkedPosts)
            }
            .refreshable { await load() }
            .task { await load() }
            .onChange(of: notificationDelegate.pendingPostID) { _, newValue in
                handleNotificationTap(newValue)
            }
            .onChange(of: posts) { _, _ in
                handleNotificationTap(notificationDelegate.pendingPostID)
            }
        }
    }

    /// 알림을 탭해서 앱이 열렸을 때 해당 글 상세 화면으로 바로 이동시킨다.
    private func handleNotificationTap(_ postID: String?) {
        guard let postID, let post = posts.first(where: { $0.id == postID }) else { return }
        showsSettings = false
        showsBookmarks = false
        navigationPath.append(post)
        notificationDelegate.pendingPostID = nil
    }

    private static let topAnchor = "top"
    private static let bottomAnchor = "bottom"

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
            posts = try await PostsRepository.shared.loadPosts()
            loadError = nil
        } catch {
            loadError = error.localizedDescription
            if posts.isEmpty {
                posts = PostsRepository.shared.loadCachedPosts()
            }
        }
    }
}

#Preview {
    ContentView()
}
