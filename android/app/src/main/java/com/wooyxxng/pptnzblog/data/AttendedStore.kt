package com.wooyxxng.pptnzblog.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 사용자가 "다녀온 공연"으로 체크한 소식을 저장한다. (블로그 북마크와 같은 역할, iOS `AttendedStore`)
 *
 * news.json의 id는 내용 해시라서 원문이 수정되면 id가 바뀔 수 있다.
 * 그래서 id만이 아니라 소식 내용 전체를 저장해두고, 모아보기는 저장된 내용으로 보여준다.
 */
class AttendedStore(context: Context) {
    private val preferences = AppPreferences(context.applicationContext)

    val itemsFlow: Flow<List<NewsItem>> = preferences.attendedItemsFlow

    suspend fun toggle(item: NewsItem) {
        val current = preferences.getAttendedItems().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current.removeAt(index) else current.add(item)
        preferences.setAttendedItems(current)
    }

    companion object {
        @Volatile private var instance: AttendedStore? = null

        fun getInstance(context: Context): AttendedStore =
            instance ?: synchronized(this) {
                instance ?: AttendedStore(context).also { instance = it }
            }
    }
}
