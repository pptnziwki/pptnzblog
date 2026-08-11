package com.wooyxxng.pptnzblog.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * posts.json을 GitHub raw URL에서 내려받아 DataStore에 캐시한다.
 * crawl.yml이 posts.json을 main 브랜치에 커밋하므로 그걸 그대로 읽는다.
 */
class PostsRepository(context: Context) {
    private val preferences = AppPreferences(context.applicationContext)
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        /** Glance 위젯이 갱신을 트리거할 때 쓰는 식별자. */
        const val WIDGET_KIND = "PptnzWidget"

        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/wooyxxng-Jang/pptnzblog/main/posts.json"

        @Volatile private var instance: PostsRepository? = null

        fun getInstance(context: Context): PostsRepository =
            instance ?: synchronized(this) {
                instance ?: PostsRepository(context).also { instance = it }
            }
    }

    /** 네트워크 우선, 실패하면 캐시로 폴백. 캐시도 없으면 에러를 던진다. */
    suspend fun loadPosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REMOTE_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body?.string() ?: throw IOException("empty body")
                val posts = json.decodeFromString<List<Post>>(body)
                preferences.savePostsCache(body)
                posts
            }
        } catch (e: Exception) {
            val cached = loadCachedPosts()
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    /** 네트워크 왕복 없이 즉시 뭔가 보여줘야 하는 위젯 placeholder/snapshot용. */
    suspend fun loadCachedPosts(): List<Post> = withContext(Dispatchers.IO) {
        val raw = preferences.loadPostsCache() ?: return@withContext emptyList()
        runCatching { json.decodeFromString<List<Post>>(raw) }.getOrDefault(emptyList())
    }
}
