package com.kduniv.aimong.feature.parent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.auth.FirebaseParentTokenProvider
import com.kduniv.aimong.core.network.model.ParentMeData
import com.kduniv.aimong.feature.auth.domain.LogoutParentUseCase
import com.kduniv.aimong.feature.parent.data.ParentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    private val parentRepository: ParentRepository,
    private val logoutParentUseCase: LogoutParentUseCase,
    private val firebaseParentTokenProvider: FirebaseParentTokenProvider
) : ViewModel() {

    private val _parentMe = MutableStateFlow<ParentMeData?>(null)
    val parentMe = _parentMe.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateToLogin = MutableSharedFlow<Unit>()
    val navigateToLogin = _navigateToLogin.asSharedFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            parentRepository.getParentMe().fold(
                onSuccess = { _parentMe.value = it },
                onFailure = { e -> _messageEvent.emit(e.message ?: "계정 정보를 불러오지 못했습니다.") }
            )
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutParentUseCase()
            _navigateToLogin.emit(Unit)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val token = firebaseParentTokenProvider.getIdTokenOrNull()
            if (token == null) {
                _messageEvent.emit("Firebase 로그인이 필요합니다.")
                return@launch
            }
            parentRepository.deleteParentAccount(token).fold(
                onSuccess = {
                    logoutParentUseCase()
                    _navigateToLogin.emit(Unit)
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "회원탈퇴에 실패했습니다.")
                }
            )
        }
    }
}
