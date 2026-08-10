import SwiftUI

struct ContentView: View {
    @State private var posts: [Post] = []
    @State private var searchText = ""
    @State private var isLoading = false
    @State private var loadError: String?

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

    var body: some View {
        NavigationStack {
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
                    List {
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
                    }
                    .listStyle(.insetGrouped)
                    .scrollContentBackground(.hidden)
                    .background(Color.pptnzBackground)
                }
            }
            .navigationTitle("PPTNZ 블로그")
            .tint(Color.pptnzCoral)
            .searchable(text: $searchText, prompt: "제목·본문 검색")
            .navigationDestination(for: Post.self) { post in
                PostDetailView(post: post)
            }
            .refreshable { await load() }
            .task { await load() }
        }
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
