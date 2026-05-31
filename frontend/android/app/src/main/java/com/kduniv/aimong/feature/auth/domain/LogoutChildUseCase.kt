package com.kduniv.aimong.feature.auth.domain

import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.auth.data.AuthRepository
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingController
import javax.inject.Inject

/**
 * 명세 순서: [DELETE /child/fcm-token] 후 [POST /child/logout], 로컬 세션 정리.
 */
class LogoutChildUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val childGachaOnboardingController: ChildGachaOnboardingController,
) {
    suspend operator fun invoke() {
        runCatching { authRepository.deleteChildFcmToken() }
        runCatching { authRepository.childLogout() }
        childGachaOnboardingController.resetActivePhase()
        sessionManager.clearSession()
    }
}
