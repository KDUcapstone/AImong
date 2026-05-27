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

/** 자녀 단원 완료 보상 FCM (`STAGE_REWARD_READY`). */
object ChildRewardNotificationHelper {

    private const val CHANNEL_ID = "aimong_child_reward"
    private const val CHANNEL_NAME = "자녀 보상 알림"

    fun showIfApplicable(context: Context, remoteMessage: RemoteMessage): Boolean {
        val type = remoteMessage.data["type"] ?: return false
        if (type != "STAGE_REWARD_READY") return false

        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: appCtx.getString(R.string.fcm_child_reward_title_default)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: appCtx.getString(R.string.fcm_child_reward_body_default)

        val intent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FCM_NOTIFICATION_TYPE, type)
            putExtra(MainActivity.EXTRA_FCM_TARGET, MainActivity.FCM_TARGET_CHILD_HOME)
        }
        val pending = PendingIntent.getActivity(
            appCtx,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_star_filled)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val stage = remoteMessage.data["stageNumber"] ?: "latest"
        nm.notify(("STAGE_REWARD_READY_$stage").hashCode(), notification)
        return true
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "자녀가 단원 완료 보상을 받을 수 있을 때 전달됩니다."
        }
        nm.createNotificationChannel(channel)
    }
}
