package com.kduniv.aimong.core.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kduniv.aimong.feature.auth.domain.RegisterChildFcmTokenUseCase
import com.kduniv.aimong.feature.auth.domain.RegisterParentFcmTokenUseCase
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 타입별 처리은 추후 (PRIVACY_ALERT, 미학습 알림 등)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            registerParentFcmTokenUseCase(fcmTokenOverride = token, requireParentSession = true)
            registerChildFcmTokenUseCase(fcmTokenOverride = token, requireChildSession = true)
        }
    }
}
