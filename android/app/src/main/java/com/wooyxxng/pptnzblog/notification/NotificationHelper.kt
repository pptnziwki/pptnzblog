package com.wooyxxng.pptnzblog.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wooyxxng.pptnzblog.MainActivity
import com.wooyxxng.pptnzblog.R
import com.wooyxxng.pptnzblog.data.Post

/**
 * 알림 채널 생성과 글 알림 발송을 담당한다. iOS `UNUserNotificationCenter` 대응.
 */
object NotificationHelper {
    const val CHANNEL_ID = "pptnzblog_posts"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            // 헤즈업(배너) 형태로 화면에 바로 뜨려면 HIGH 이상의 importance가 필요하다.
            NotificationManager.IMPORTANCE_HIGH,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun showPostNotification(context: Context, post: Post, notificationId: Int = post.numericID) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("pptnzblog://post/${post.id}"), context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // 상태표시줄 아이콘은 알파 채널만 사용해 실루엣으로 그려진다. adaptive icon의
            // foreground 레이어는 안전 영역 여백이 넓어 그대로 쓰면 실루엣이 작게 보이므로,
            // 투명 여백을 잘라내 캔버스를 꽉 채운 전용 알림 아이콘을 사용한다.
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(post.title)
            .setContentText(post.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(post.content))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
