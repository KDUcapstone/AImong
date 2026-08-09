package com.kduniv.aimong.core.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R

/**
 * v2.1 가챠 레벨업 부모 FCM (`type=GACHA_LEVEL_UP`).
 * 서버가 `notification` 페이로드와 `data`를 함께 보낼 수 있어 둘 다 수용한다.
 */
object GachaLevelUpNotificationHelper {

    private const val CHANNEL_ID = "aimong_gacha_level_up"
    private const val CHANNEL_NAME = "가챠 레벨업"

    fun showIfApplicable(context: Context, remoteMessage: RemoteMessage): Boolean {
        val type = remoteMessage.data["type"] ?: return false
        if (type != "GACHA_LEVEL_UP") return false

        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: appCtx.getString(R.string.fcm_gacha_level_up_title_default)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: appCtx.getString(R.string.fcm_gacha_level_up_body_default)

        val pullCount = remoteMessage.data["gachaPullCount"]
        val text = if (!pullCount.isNullOrBlank()) {
            "$body (${appCtx.getString(R.string.fcm_gacha_level_up_pull_count_fmt, pullCount)})"
        } else {
            body
        }

        val intent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FCM_NOTIFICATION_TYPE, type)
            putExtra(MainActivity.EXTRA_FCM_TARGET, MainActivity.FCM_TARGET_CHILD_GACHA)
        }
        val pending = PendingIntent.getActivity(
            appCtx,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_star_filled)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val id = ("GACHA_LEVEL_UP_" + (pullCount ?: "0")).hashCode()
        nm.notify(id, notification)
        return true
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "자녀 가챠 레벨 구간 상승 시 부모에게 전달되는 알림입니다."
        }
        nm.createNotificationChannel(ch)
    }
}
