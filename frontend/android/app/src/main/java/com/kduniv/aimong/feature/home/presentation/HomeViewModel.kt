package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
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
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val missionRepository: MissionRepository,
    private val apiService: AimongApiService,
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

    /** 퀘스트 수령 등으로 서버가 준 티켓 보유량만 반영 (전체 홈 재로드 없음) */
    fun applyRemainingTickets(normal: Int, rare: Int, epic: Int) {
        _uiState.update { s ->
            val desc = if (normal == 0 && rare == 0 && epic == 0) ""
            else "일반 $normal · 레어 $rare · 에픽 $epic"
            s.copy(
                normalTickets = normal,
                srBonus = rare + epic,
                gachaDescription = desc,
                topTicketCount = normal + rare + epic
            )
        }
    }

    fun onCheckReturnReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(subtleNotice = null, errorMessage = null) }
            runCatching { apiService.getReturnReward() }
                .onSuccess { response ->
                    if (response.success) {
                        val d = response.data
                        val msg = if (!d.hasReward) {
                            "복귀 보상 없음"
                        } else {
                            val days = d.daysMissed ?: 0
                            val tickets = d.ticketCount ?: 0
                            d.message ?: "복귀 보상 있음: ${days}일 결석 · 티켓 $tickets"
                        }
                        _uiState.update { it.copy(subtleNotice = msg) }
                    } else {
                        val code = response.error?.code ?: "UNKNOWN"
                        val message = response.error?.message ?: "요청을 처리하지 못했습니다."
                        _uiState.update {
                            it.copy(errorMessage = "[$code] $message")
                        }
                    }
                }
                .onFailure { e ->
                    val msg = when (e) {
                        is HttpException -> ApiErrorMapper.userMessageForHttpException(e)
                        is IOException -> "연결을 확인한 뒤 다시 시도해주세요."
                        else -> e.message ?: "복귀 보상 조회에 실패했습니다."
                    }
                    _uiState.update { it.copy(errorMessage = msg) }
                }
        }
    }

    fun onClaimReturnReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(subtleNotice = null, errorMessage = null) }
            runCatching { apiService.claimReturnReward() }
                .onSuccess { response ->
                    if (response.success) {
                        val t = response.data.remainingTickets
                        applyRemainingTickets(t.normal, t.rare, t.epic)
                        _uiState.update { it.copy(subtleNotice = "복귀 보상 수령 완료") }
                    } else {
                        val code = response.error?.code ?: "UNKNOWN"
                        val message = response.error?.message ?: "요청을 처리하지 못했습니다."
                        _uiState.update {
                            it.copy(errorMessage = "[$code] $message")
                        }
                    }
                }
                .onFailure { e ->
                    val msg = when (e) {
                        is HttpException -> ApiErrorMapper.userMessageForHttpException(e)
                        is IOException -> "연결을 확인한 뒤 다시 시도해주세요."
                        else -> e.message ?: "복귀 보상 수령에 실패했습니다."
                    }
                    _uiState.update { it.copy(errorMessage = msg) }
                }
        }
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
