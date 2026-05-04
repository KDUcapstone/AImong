package com.kduniv.aimong.feature.auth.domain

import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.feature.auth.data.AuthRepository
import javax.inject.Inject

class ChildLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): Result<ChildLoginResponse> =
        authRepository.loginChild(code)
}
