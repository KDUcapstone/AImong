package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.home.domain.HomePathBuilder
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val missionRepository: MissionRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val homePrefs by lazy {
        appContext.getSharedPreferences(PREFS_HOME, Context.MODE_PRIVATE)
    }

    fun onHomeResumed() {
        loadHome()
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSubtleNotice() {
        _uiState.update { it.copy(subtleNotice = null) }
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, subtleNotice = null) }
            getHomeStatusUseCase().fold(
                onSuccess = { data ->
                    missionRepository.refreshMissions()
                    val missions = missionRepository.getMissionsFlow().first()
                    val path = HomePathBuilder.build(data, missions)
                    val notice = computeServerDayNotice(data.serverDate)
                    val ui = HomeUiMapper.toUiState(data).copy(
                        pathItems = path,
                        isLoading = false,
                        errorMessage = null,
                        subtleNotice = notice
                    )
                    _uiState.value = ui
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message?.takeIf { m -> m.isNotBlank() }
                                ?: "홈 정보를 불러오지 못했습니다."
                        )
                    }
                }
            )
        }
    }

    /** GET /home 의 serverDate(KST)가 바뀌었으면 짧은 안내 (백그라운드 워커 없이 저장 비교) */
    private fun computeServerDayNotice(serverDate: String?): String? {
        if (serverDate.isNullOrBlank()) return null
        val last = homePrefs.getString(KEY_LAST_SERVER_DATE, null)
        homePrefs.edit().putString(KEY_LAST_SERVER_DATE, serverDate).apply()
        return if (last != null && last != serverDate) {
            appContext.getString(R.string.home_notice_server_day_changed)
        } else {
            null
        }
    }

    fun getProfileLabel(type: String): String {
        return when (type) {
            "SPROUT" -> "AI 새싹"
            "EXPLORER" -> "AI 탐험가"
            "CRITIC" -> "AI 비평가"
            "GUARDIAN" -> "AI 수호자"
            else -> "AI 입문자"
        }
    }

    companion object {
        private const val PREFS_HOME = "aimong_home"
        private const val KEY_LAST_SERVER_DATE = "last_server_date_kst"
    }
}
