package com.kduniv.aimong.feature.auth.presentation

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.feature.auth.domain.RegisterChildUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ParentRegisterViewModel @Inject constructor(
    private val registerChildUseCase: RegisterChildUseCase
) : ViewModel() {

    suspend fun registerChildProfile(nickname: String): Result<ParentRegisterResponse> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("Google 로그인이 필요합니다."))
        val token = user.getIdToken(false).await().token
            ?: return Result.failure(IllegalStateException("인증 토큰을 가져오지 못했습니다."))
        return registerChildUseCase(nickname.trim(), token)
    }
}
