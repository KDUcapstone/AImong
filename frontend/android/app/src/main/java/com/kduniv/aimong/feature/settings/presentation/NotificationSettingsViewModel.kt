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
import kotlinx.coroutines.Job
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

    private val _isParentRole = MutableStateFlow(false)
    val isParentRole = _isParentRole.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private var saveJob: Job? = null

    fun load() {
        viewModelScope.launch {
            val role = sessionManager.userRole.first()
            val isParent = role == "PARENT"
            _isParentRole.value = isParent
            // 부모·자녀 각각 JWT로 본인 알림 설정을 GET·PATCH (서버에 자녀별·부모별로 분리 저장).
            _canEdit.value = role == "PARENT" || role == "CHILD"
            repo.getSettings().fold(
                onSuccess = { _settings.value = it },
                onFailure = { _messageEvent.emit(it.message ?: "알림 설정을 불러오지 못했습니다.") }
            )
        }
    }

    fun save(patch: NotificationSettingsPatchRequest) {
        if (!_canEdit.value) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _isSaving.value = true
            repo.patchSettings(patch).fold(
                onSuccess = {
                    _settings.value = it
                    _messageEvent.emit("저장되었습니다.")
                },
                onFailure = { _messageEvent.emit(it.message ?: "저장에 실패했습니다.") }
            )
            _isSaving.value = false
        }
    }
}

