package com.kduniv.aimong.feature.auth.domain

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.kduniv.aimong.BuildConfig
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.auth.data.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * POST /child/fcm-token — DataStore에 자녀 JWT가 있을 때만 호출.
 * [requireChildSession] true: 역할이 CHILD일 때(앱 실행·FCM 토큰 갱신).
 * 로그인 직후 [ChildLoginViewModel]에서는 세션 저장 뒤 true로 호출.
 */
class RegisterChildFcmTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        fcmTokenOverride: String? = null,
        requireChildSession: Boolean = true
    ) {
        runCatching {
            if (requireChildSession) {
                val role = sessionManager.userRole.first()
                if (role != "CHILD") return@runCatching
            }
            val jwt = sessionManager.authToken.first()
            if (jwt.isNullOrBlank()) return@runCatching
            val fcm = fcmTokenOverride ?: FirebaseMessaging.getInstance().token.await()
            authRepository.registerChildFcmToken(fcm).fold(
                onSuccess = {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Child FCM token registered: ${mask(fcm)}")
                },
                onFailure = {
                    if (BuildConfig.DEBUG) Log.w(TAG, "Child FCM token registration failed", it)
                },
            )
        }.onFailure {
            if (BuildConfig.DEBUG) Log.w(TAG, "Child FCM token registration skipped/failed", it)
        }
    }

    private fun mask(token: String): String {
        if (token.length <= 10) return "***"
        return "${token.take(6)}...${token.takeLast(4)}"
    }

    companion object {
        private const val TAG = "AimongFcmToken"
    }
}
