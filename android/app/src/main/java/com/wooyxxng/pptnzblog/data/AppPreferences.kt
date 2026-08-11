package com.wooyxxng.pptnzblog.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

val Context.appDataStore by preferencesDataStore(name = "pptnzblog_prefs")

/** 알림 시각 하나. iOS `NotificationSettings.DailyTime`에 대응. */
@Serializable
data class DailyTime(val id: String = UUID.randomUUID().toString(), val hour: Int, val minute: Int)

/**
 * posts.json 캐시, 북마크, 알림 설정 등을 DataStore(Preferences)에 저장한다.
 * 앱/위젯이 같은 프로세스에서 동작하므로 iOS의 App Group UserDefaults와 달리
 * 별도 공유 컨테이너 없이 이 DataStore 인스턴스 하나면 충분하다.
 */
class AppPreferences(context: Context) {
    private val context = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val POSTS_CACHE = stringPreferencesKey("posts_cache_json")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_TIMES = stringPreferencesKey("notifications_daily_times")
        val LAST_SEEN_POST_ID = intPreferencesKey("last_seen_post_id")
        val SCHEDULED_DATES = stringPreferencesKey("daily_random_post_scheduled_dates")
        val BOOKMARKED_IDS = stringSetPreferencesKey("bookmarked_post_ids")
    }

    companion object {
        const val MAX_DAILY_TIMES = 3
    }

    // MARK: posts.json 캐시 (원문 그대로 저장, 다시 인코딩하지 않음)

    suspend fun savePostsCache(rawJson: String) {
        context.appDataStore.edit { it[Keys.POSTS_CACHE] = rawJson }
    }

    suspend fun loadPostsCache(): String? =
        context.appDataStore.data.map { it[Keys.POSTS_CACHE] }.first()

    // MARK: 알림 설정

    val notificationsEnabledFlow: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun getNotificationsEnabled(): Boolean = notificationsEnabledFlow.first()

    val dailyTimesFlow: Flow<List<DailyTime>> = context.appDataStore.data.map { prefs ->
        prefs[Keys.DAILY_TIMES]?.let { raw ->
            runCatching { json.decodeFromString<List<DailyTime>>(raw) }.getOrNull()
        } ?: listOf(DailyTime(hour = 9, minute = 0))
    }

    suspend fun getDailyTimes(): List<DailyTime> = dailyTimesFlow.first()

    suspend fun setDailyTimes(times: List<DailyTime>) {
        context.appDataStore.edit { it[Keys.DAILY_TIMES] = json.encodeToString(times) }
    }

    // MARK: 신규 글 감지 기준점

    suspend fun getLastSeenPostId(): Int =
        context.appDataStore.data.map { it[Keys.LAST_SEEN_POST_ID] ?: 0 }.first()

    suspend fun setLastSeenPostId(id: Int) {
        context.appDataStore.edit { it[Keys.LAST_SEEN_POST_ID] = id }
    }

    // MARK: 일일 랜덤 글 알림의 마지막 예약 시각(초 단위 epoch millis)

    suspend fun getScheduledDates(): Map<String, Long> {
        val raw = context.appDataStore.data.map { it[Keys.SCHEDULED_DATES] }.first() ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Long>>(raw) }.getOrDefault(emptyMap())
    }

    suspend fun setScheduledDates(dates: Map<String, Long>) {
        context.appDataStore.edit { it[Keys.SCHEDULED_DATES] = json.encodeToString(dates) }
    }

    // MARK: 북마크

    val bookmarkedIdsFlow: Flow<Set<String>> =
        context.appDataStore.data.map { it[Keys.BOOKMARKED_IDS] ?: emptySet() }

    suspend fun getBookmarkedIds(): Set<String> = bookmarkedIdsFlow.first()

    suspend fun setBookmarkedIds(ids: Set<String>) {
        context.appDataStore.edit { it[Keys.BOOKMARKED_IDS] = ids }
    }
}
