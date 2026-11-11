package com.wooyxxng.pptnzblog.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wooyxxng.pptnzblog.data.AppPreferences
import com.wooyxxng.pptnzblog.data.PostsRepository
import java.util.concurrent.TimeUnit

/**
 * 주기적으로 posts.json을 확인해 새 글이 있으면 알림을 보낸다.
 * iOS `BackgroundRefreshManager`(BGTaskScheduler)에 대응. WorkManager 최소 주기 제약(15분)을 따른다.
 */
class BackgroundRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        // 새 소식(news) 감지는 블로그 새 글 확인과 별개로, 실패해도 서로 영향을 주지 않게 한다.
        runCatching { NewsNotifier.checkForNew(applicationContext) }
        if (!preferences.getNotificationsEnabled()) return Result.success()

        return try {
            val posts = PostsRepository.getInstance(applicationContext).loadPosts()
            val latest = posts.maxByOrNull { it.numericID } ?: return Result.success()
            val lastSeenId = preferences.getLastSeenPostId()

            if (lastSeenId == 0) {
                // 첫 실행: 알림 없이 기준점만 저장한다.
                preferences.setLastSeenPostId(latest.numericID)
                return Result.success()
            }

            if (latest.numericID > lastSeenId) {
                NotificationHelper.showPostNotification(applicationContext, latest)
            }
            preferences.setLastSeenPostId(latest.numericID)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "background_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackgroundRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
