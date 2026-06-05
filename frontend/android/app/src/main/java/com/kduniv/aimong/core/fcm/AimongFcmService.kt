package com.kduniv.aimong.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kduniv.aimong.feature.auth.domain.RegisterChildFcmTokenUseCase
import com.kduniv.aimong.feature.auth.domain.RegisterParentFcmTokenUseCase
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AimongFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var registerParentFcmTokenUseCase: RegisterParentFcmTokenUseCase

    @Inject
    lateinit var registerChildFcmTokenUseCase: RegisterChildFcmTokenUseCase

    @Inject
    lateinit var parentDashboardRefreshBus: ParentDashboardRefreshBus

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (GachaLevelUpNotificationHelper.showIfApplicable(this, remoteMessage)) {
            return
        }
        if (QuestCompleteRequestNotificationHelper.handleIfApplicable(
                this,
                remoteMessage,
                parentDashboardRefreshBus,
            )
        ) {
            return
        }
        if (PrivacyAlertNotificationHelper.showIfApplicable(this, remoteMessage)) {
            return
        }
        if (LearningReminderNotificationHelper.showIfApplicable(this, remoteMessage)) {
            return
        }
        if (WeeklyReportNotificationHelper.showIfApplicable(this, remoteMessage)) {
            return
        }
        if (ChildRewardNotificationHelper.showIfApplicable(this, remoteMessage)) {
            return
        }
        if (StreakShieldNotificationHelper.showIfApplicable(this, remoteMessage)) {
            return
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            try {
                registerParentFcmTokenUseCase(fcmTokenOverride = token, requireParentSession = true)
                registerChildFcmTokenUseCase(fcmTokenOverride = token, requireChildSession = true)
            } catch (e: Exception) {
                Log.e(TAG, "FCM token registration failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "AimongFcm"
    }
}
