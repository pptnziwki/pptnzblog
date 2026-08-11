package com.wooyxxng.pptnzblog

import android.app.Application
import com.wooyxxng.pptnzblog.notification.BackgroundRefreshWorker
import com.wooyxxng.pptnzblog.notification.DailyPostNotifier
import com.wooyxxng.pptnzblog.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 시작 시 알림 채널을 만들고 백그라운드 새로고침/일일 알림을 예약한다.
 * iOS `PPTNZBlogApp`의 `.onAppear` 초기화 로직에 대응.
 */
class PptnzBlogApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        BackgroundRefreshWorker.schedule(this)

        appScope.launch {
            DailyPostNotifier.reschedule(this@PptnzBlogApp)
        }
    }
}
