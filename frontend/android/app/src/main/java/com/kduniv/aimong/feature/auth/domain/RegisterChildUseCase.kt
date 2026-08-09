package com.kduniv.aimong.feature.auth.domain

import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.feature.auth.data.AuthRepository
import javax.inject.Inject

class RegisterChildUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        nickname: String,
        firebaseIdToken: String
    ): Result<ParentRegisterResponse> =
        authRepository.registerParentChild(nickname, firebaseIdToken)
}
