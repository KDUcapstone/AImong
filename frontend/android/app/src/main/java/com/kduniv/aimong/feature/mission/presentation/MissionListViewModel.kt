package com.kduniv.aimong.feature.mission.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionProgress
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class MissionListViewModel @Inject constructor(
    private val missionRepository: MissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MissionListUiState>(MissionListUiState.Loading)
    val uiState: StateFlow<MissionListUiState> = _uiState

    init {
        observeMissions()
        refreshMissions()
    }

    private fun observeMissions() {
        viewModelScope.launch {
            missionRepository.getMissionsFlow()
                .flowOn(Dispatchers.IO)
                .collect { missions ->
                    val currentState = _uiState.value
                    if (currentState is MissionListUiState.Success) {
                        _uiState.value = currentState.copy(missions = missions)
                    } else {
                        // 첫 로컬 데이터 로드 시 Success 상태로 전환
                        _uiState.value = MissionListUiState.Success(missions, MissionProgress(0, 0, 0))
                    }
                }
        }
    }

    fun refreshMissions() {
        viewModelScope.launch {
            try {
                // 최대 5초 대기 후 타임아웃 처리하여 무한 로딩 방지
                withTimeout(5000) {
                    missionRepository.refreshMissions()
                        .onSuccess { progress ->
                            val currentState = _uiState.value
                            if (currentState is MissionListUiState.Success) {
                                _uiState.value = currentState.copy(progress = progress)
                            } else if (currentState is MissionListUiState.Loading) {
                                // API 응답이 로컬 DB보다 빠를 경우
                                _uiState.value = MissionListUiState.Success(emptyList(), progress)
                            }
                        }
                        .onFailure {
                            if (_uiState.value is MissionListUiState.Loading) {
                                _uiState.value = MissionListUiState.Error(it.message ?: "데이터를 불러오지 못했습니다.")
                            }
                        }
                }
            } catch (e: Exception) {
                if (_uiState.value is MissionListUiState.Loading) {
                    _uiState.value = MissionListUiState.Error("네트워크 연결이 원활하지 않습니다.")
                }
            }
        }
    }
}

sealed class MissionListUiState {
    object Loading : MissionListUiState()
    data class Success(val missions: List<Mission>, val progress: MissionProgress) : MissionListUiState()
    data class Error(val message: String) : MissionListUiState()
}
