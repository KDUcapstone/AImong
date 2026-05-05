package com.kduniv.aimong.feature.auth.presentation

import androidx.lifecycle.ViewModel
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.auth.domain.ChildLoginUseCase
import com.kduniv.aimong.feature.auth.domain.RegisterChildFcmTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChildLoginViewModel @Inject constructor(
    private val childLoginUseCase: ChildLoginUseCase,
    private val sessionManager: SessionManager,
    private val registerChildFcmTokenUseCase: RegisterChildFcmTokenUseCase
) : ViewModel() {

    suspend fun loginWithCode(code: String): Result<Unit> {
        val result = childLoginUseCase(code.trim())
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("로그인에 실패했습니다."))
        }
        val data = result.getOrThrow()
        sessionManager.saveSession("CHILD", 1, data.sessionToken)
        registerChildFcmTokenUseCase(requireChildSession = true)
        return Result.success(Unit)
    }
}
