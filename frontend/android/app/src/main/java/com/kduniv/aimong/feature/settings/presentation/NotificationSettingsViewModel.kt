package com.kduniv.aimong.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.model.NotificationSettingsData
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.settings.data.NotificationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repo: NotificationSettingsRepository,
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _settings = MutableStateFlow<NotificationSettingsData?>(null)
    val settings = _settings.asStateFlow()

    private val _canEdit = MutableStateFlow(false)
    val canEdit = _canEdit.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _canEdit.value = sessionManager.userRole.first() == "PARENT"
            repo.getSettings().fold(
                onSuccess = { _settings.value = it },
                onFailure = { _messageEvent.emit(it.message ?: "알림 설정을 불러오지 못했습니다.") }
            )
        }
    }

    fun save(patch: NotificationSettingsPatchRequest) {
        if (!_canEdit.value) return
        viewModelScope.launch {
            repo.patchSettings(patch).fold(
                onSuccess = {
                    _settings.value = it
                    _messageEvent.emit("저장되었습니다.")
                },
                onFailure = { _messageEvent.emit(it.message ?: "저장에 실패했습니다.") }
            )
        }
    }
}

