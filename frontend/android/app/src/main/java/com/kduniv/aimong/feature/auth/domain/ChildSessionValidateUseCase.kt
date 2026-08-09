package com.kduniv.aimong.feature.auth.domain

import com.kduniv.aimong.feature.auth.data.AuthRepository
import javax.inject.Inject

/**
 * [GET /child/me] — CHILD 세션·session_version 유효성 확인.
 */
class ChildSessionValidateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.getChildMe()
}
