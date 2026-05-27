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

/** 부모 개인정보 위험 알림 FCM (`PRIVACY_ALERT`, `PRIVACY_ALERT_BATCH`). */
object PrivacyAlertNotificationHelper {

    private const val CHANNEL_ID = "aimong_privacy_alert"
    private const val CHANNEL_NAME = "개인정보 알림"

    fun showIfApplicable(context: Context, remoteMessage: RemoteMessage): Boolean {
        val type = remoteMessage.data["type"] ?: return false
        if (type != "PRIVACY_ALERT" && type != "PRIVACY_ALERT_BATCH") return false

        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: if (type == "PRIVACY_ALERT_BATCH") {
                appCtx.getString(R.string.fcm_privacy_alert_batch_title_default)
            } else {
                appCtx.getString(R.string.fcm_privacy_alert_title_default)
            }
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: if (type == "PRIVACY_ALERT_BATCH") {
                appCtx.getString(R.string.fcm_privacy_alert_batch_body_default)
            } else {
                appCtx.getString(R.string.fcm_privacy_alert_body_default)
            }

        val intent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FCM_NOTIFICATION_TYPE, type)
            putExtra(MainActivity.EXTRA_FCM_TARGET, MainActivity.FCM_TARGET_PARENT_DASHBOARD)
        }
        val pending = PendingIntent.getActivity(
            appCtx,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val idSeed = remoteMessage.data["alertId"]
            ?: remoteMessage.data["batchId"]
            ?: remoteMessage.data["childId"]
            ?: type
        nm.notify((type + idSeed).hashCode(), notification)
        return true
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "자녀 대화에서 개인정보 위험이 감지됐을 때 부모에게 전달됩니다."
        }
        nm.createNotificationChannel(channel)
    }
}
