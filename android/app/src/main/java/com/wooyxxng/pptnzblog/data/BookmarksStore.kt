package com.wooyxxng.pptnzblog.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.wooyxxng.pptnzblog.widget.PptnzGlanceWidget
import kotlinx.coroutines.flow.Flow

/**
 * 즐겨찾기(북마크)한 글 id를 DataStore에 저장한다.
 * 북마크함을 보여주는 위젯 옵션이 있어서, 토글할 때마다 위젯 갱신을 트리거한다.
 */
class BookmarksStore(context: Context) {
    private val context = context.applicationContext
    private val preferences = AppPreferences(this.context)

    val bookmarkedIdsFlow: Flow<Set<String>> = preferences.bookmarkedIdsFlow

    suspend fun isBookmarked(post: Post): Boolean = preferences.getBookmarkedIds().contains(post.id)

    suspend fun toggle(post: Post) {
        val current = preferences.getBookmarkedIds().toMutableSet()
        if (current.contains(post.id)) {
            current.remove(post.id)
        } else {
            current.add(post.id)
        }
        preferences.setBookmarkedIds(current)
        // 북마크함을 노출하는 위젯이 있을 수 있으니 즉시 갱신을 요청한다.
        runCatching { PptnzGlanceWidget().updateAll(context) }
    }

    companion object {
        @Volatile private var instance: BookmarksStore? = null

        fun getInstance(context: Context): BookmarksStore =
            instance ?: synchronized(this) {
                instance ?: BookmarksStore(context).also { instance = it }
            }
    }
}
