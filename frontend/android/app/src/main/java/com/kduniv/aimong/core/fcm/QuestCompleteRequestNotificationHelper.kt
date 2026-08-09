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
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshBus
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshTrigger

/**
 * 자녀 커스텀 퀘스트 완료 요청 FCM (`type=QUEST_COMPLETE_REQUEST`).
 * 포그라운드에서도 [ParentDashboardRefreshBus]로 실세계 미션 목록을 즉시 갱신한다.
 */
object QuestCompleteRequestNotificationHelper {

    private const val CHANNEL_ID = "aimong_quest_complete_request"
    private const val CHANNEL_NAME = "실세계 미션 승인"

    fun handleIfApplicable(
        context: Context,
        remoteMessage: RemoteMessage,
        refreshBus: ParentDashboardRefreshBus,
    ): Boolean {
        val type = remoteMessage.data["type"] ?: return false
        if (type != "QUEST_COMPLETE_REQUEST") return false

        val childId = remoteMessage.data["childId"]?.trim()?.takeIf { it.isNotEmpty() }
        refreshBus.notify(
            ParentDashboardRefreshTrigger.CustomQuestsChanged(
                childId = childId,
                showPendingNotice = true,
            ),
        )

        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: appCtx.getString(R.string.fcm_quest_complete_request_title_default)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: appCtx.getString(R.string.fcm_quest_complete_request_body_default)

        val intent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_PARENT_CUSTOM_QUESTS, true)
            childId?.let { putExtra(MainActivity.EXTRA_QUEST_COMPLETE_CHILD_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            appCtx,
            0,
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notifyId = ("QUEST_COMPLETE_REQUEST_" + (childId ?: "default")).hashCode()
        nm.notify(notifyId, notification)
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
            description = "자녀가 실세계 미션 완료를 요청했을 때 부모에게 전달됩니다."
        }
        nm.createNotificationChannel(channel)
    }
}
