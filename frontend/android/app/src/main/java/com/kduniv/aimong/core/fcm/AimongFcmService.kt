package com.kduniv.aimong.core.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// TODO: FCM 수신 처리
// - 개인정보 알림 (PRIVACY_ALERT)
// - 미학습 알림
// - 레벨업 알림
// - 주간 리포트 알림
class AimongFcmService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO: data payload 타입별 분기 처리
    }
    override fun onNewToken(token: String) {
        // TODO: 새 토큰을 서버에 등록
    }
}
