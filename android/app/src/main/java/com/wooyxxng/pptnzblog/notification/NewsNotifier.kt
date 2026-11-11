package com.wooyxxng.pptnzblog.notification

import android.content.Context
import com.wooyxxng.pptnzblog.data.AppPreferences
import com.wooyxxng.pptnzblog.data.NewsItem
import com.wooyxxng.pptnzblog.data.NewsRepository

/**
 * news 탭에 새 소식이 올라왔는지 감지해 알림을 띄운다. (iOS `NewsNotifier`)
 *
 * 블로그는 글 id가 숫자라서 "가장 큰 id"만 기억하면 되지만, news는 고유 번호가 없고
 * 내용 해시가 id라서 "이미 본 id 집합"을 저장해두고 거기에 없는 항목을 새 소식으로 본다.
 */
object NewsNotifier {

    /** 백그라운드 새로고침에서 호출. 새 소식이 있으면 알림을 보낸다. */
    suspend fun checkForNew(context: Context) {
        val preferences = AppPreferences(context)
        val items = NewsRepository.getInstance(context).loadNews()
        if (items.isEmpty()) return

        val seen = preferences.getSeenNewsIds()
        if (seen == null) {
            // 최초 실행: 기존 소식 수백 건이 알림으로 쏟아지지 않도록 기준점만 저장한다.
            preferences.setSeenNewsIds(items.map { it.id }.toSet())
            return
        }

        val newItems = items.filter { it.id !in seen }
        if (newItems.isEmpty()) return

        // 알림이 꺼져 있어도 본 것으로 처리해서, 나중에 다시 켰을 때 밀린 소식이 한꺼번에 오지 않게 한다.
        preferences.setSeenNewsIds(seen + items.map { it.id })
        if (!preferences.getNotificationsEnabled()) return

        NotificationHelper.showNewsNotification(context, newItems)
    }

    /**
     * 사용자가 NEWS 탭에서 목록을 직접 본 경우, 그 소식들은 이미 본 것으로 기록한다.
     * (이미 본 소식이 나중에 백그라운드 새로고침에서 "새 소식" 알림으로 오는 걸 막는다.)
     */
    suspend fun markSeen(context: Context, items: List<NewsItem>) {
        if (items.isEmpty()) return
        val preferences = AppPreferences(context)
        preferences.setSeenNewsIds((preferences.getSeenNewsIds() ?: emptySet()) + items.map { it.id })
    }
}
