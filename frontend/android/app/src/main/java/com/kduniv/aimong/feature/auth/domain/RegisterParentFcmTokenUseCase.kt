package com.kduniv.aimong.feature.auth.domain

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.auth.data.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * POST /parent/fcm-token — Google(부모) Firebase 세션이 있을 때만 서버에 FCM 토큰을 보냅니다.
 * [requireParentSession] true: DataStore 역할이 PARENT일 때만(앱 재시작·토큰 갱신).
 * false: Google 로그인 직후(아직 닉네임 등록 전·역할 미저장).
 */
class RegisterParentFcmTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        fcmTokenOverride: String? = null,
        requireParentSession: Boolean = true
    ) {
        runCatching {
            if (requireParentSession) {
                val role = sessionManager.userRole.first()
                if (role != "PARENT") return@runCatching
            }
            val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching
            val idToken = user.getIdToken(false).await().token ?: return@runCatching
            val fcm = fcmTokenOverride ?: FirebaseMessaging.getInstance().token.await()
            authRepository.registerParentFcmToken(idToken, fcm)
        }
    }
}
