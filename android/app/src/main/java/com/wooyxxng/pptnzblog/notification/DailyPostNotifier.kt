package com.wooyxxng.pptnzblog.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wooyxxng.pptnzblog.data.AppPreferences
import com.wooyxxng.pptnzblog.data.DailyTime
import com.wooyxxng.pptnzblog.data.PostsRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 설정된 시각(최대 3개)마다 랜덤 글 알림을 보낸다.
 * iOS `DailyPostNotifier`(UNCalendarNotificationTrigger)의 "1회성 예약 → 발동 시 재예약" 패턴을
 * WorkManager의 OneTimeWorkRequest + initialDelay로 구현했다.
 */
object DailyPostNotifier {
    private const val TAG = "daily_post_notifier"

    private fun uniqueWorkName(id: String) = "daily_post_$id"

    suspend fun reschedule(context: Context) {
        val preferences = AppPreferences(context)
        val workManager = WorkManager.getInstance(context)
        val enabled = preferences.getNotificationsEnabled()
        val times = preferences.getDailyTimes()

        if (!enabled || times.isEmpty()) {
            workManager.cancelAllWorkByTag(TAG)
            preferences.setScheduledDates(emptyMap())
            return
        }

        // 삭제된 시각의 예약은 정리한다.
        val currentIds = times.map { it.id }.toSet()
        val scheduledDates = preferences.getScheduledDates().toMutableMap()
        val staleIds = scheduledDates.keys.filter { it !in currentIds }
        staleIds.forEach { staleId ->
            workManager.cancelUniqueWork(uniqueWorkName(staleId))
            scheduledDates.remove(staleId)
        }
        preferences.setScheduledDates(scheduledDates)

        times.forEach { scheduleOne(context, it) }
    }

    suspend fun scheduleOne(context: Context, time: DailyTime) {
        val delayMillis = nextFireDelayMillis(time.hour, time.minute)
        val data = workDataOf("id" to time.id, "hour" to time.hour, "minute" to time.minute)
        val request = OneTimeWorkRequestBuilder<DailyPostNotifierWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName(time.id), ExistingWorkPolicy.REPLACE, request)

        val preferences = AppPreferences(context)
        val scheduledDates = preferences.getScheduledDates().toMutableMap()
        scheduledDates[time.id] = System.currentTimeMillis() + delayMillis
        preferences.setScheduledDates(scheduledDates)
    }

    private fun nextFireDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}

class DailyPostNotifierWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.failure()

        val posts = runCatching { PostsRepository.getInstance(applicationContext).loadPosts() }.getOrDefault(emptyList())
        if (posts.isNotEmpty()) {
            val randomPost = posts.random()
            NotificationHelper.showPostNotification(applicationContext, randomPost, notificationId = id.hashCode())
        }

        // 이 시각이 설정에서 삭제되지 않았다면 다음 날을 위해 재예약한다.
        val preferences = AppPreferences(applicationContext)
        val time = preferences.getDailyTimes().find { it.id == id }
        if (time != null) {
            DailyPostNotifier.scheduleOne(applicationContext, time)
        }
        return Result.success()
    }
}
