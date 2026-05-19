package com.kduniv.aimong.feature.auth.domain

import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.core.auth.FirebaseParentTokenProvider
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.parent.data.ParentRepository
import javax.inject.Inject

/**
 * v2.3: DELETE /parent/fcm-token → POST /parent/logout → FirebaseAuth.signOut() → 로컬 정리.
 * Firebase 로컬 세션 종료는 FE, BE는 FCM·부모 세션 메타만 처리.
 */
class LogoutParentUseCase @Inject constructor(
    private val parentRepository: ParentRepository,
    private val firebaseParentTokenProvider: FirebaseParentTokenProvider,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() {
        runCatching {
            firebaseParentTokenProvider.getIdTokenOrNull()?.let { token ->
                parentRepository.deleteParentFcmToken(token)
                parentRepository.parentLogout(token)
            }
        }
        runCatching { FirebaseAuth.getInstance().signOut() }
        sessionManager.clearSession()
    }
}
