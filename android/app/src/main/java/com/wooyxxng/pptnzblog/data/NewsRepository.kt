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
 * news.json을 GitHub raw URL에서 내려받아 DataStore에 캐시한다. (iOS `NewsRepository`)
 * PostsRepository와 같은 방식(네트워크 우선, 실패하면 캐시)으로 동작한다.
 */
class NewsRepository(context: Context) {
    private val preferences = AppPreferences(context.applicationContext)
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/wooyxxng-Jang/pptnzblog/main/news.json"

        @Volatile private var instance: NewsRepository? = null

        fun getInstance(context: Context): NewsRepository =
            instance ?: synchronized(this) {
                instance ?: NewsRepository(context).also { instance = it }
            }
    }

    /** 네트워크 우선, 실패하면 캐시로 폴백. 캐시도 없으면 에러를 던진다. */
    suspend fun loadNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(REMOTE_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body?.string() ?: throw IOException("empty body")
                val items = json.decodeFromString<List<NewsItem>>(body)
                preferences.saveNewsCache(body)
                items
            }
        } catch (e: Exception) {
            val cached = loadCachedNews()
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    suspend fun loadCachedNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        val raw = preferences.loadNewsCache() ?: return@withContext emptyList()
        runCatching { json.decodeFromString<List<NewsItem>>(raw) }.getOrDefault(emptyList())
    }
}
