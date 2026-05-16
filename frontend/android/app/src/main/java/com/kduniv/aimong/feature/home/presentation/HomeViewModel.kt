package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.home.domain.HomePathBuilder
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
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
    private val homeRepository: HomeRepository,
    private val walletRepository: WalletRepository,
    private val appBootstrapRepository: AppBootstrapRepository,
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
                rareEpicTicketCount = rare + epic,
                gachaDescription = desc,
                topTicketCount = normal + rare + epic
            )
        }
    }

    fun onCheckReturnReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(subtleNotice = null, errorMessage = null) }
            homeRepository.getReturnReward().fold(
                onSuccess = { d ->
                    val msg = if (!d.hasReward) {
                        "복귀 보상 없음"
                    } else {
                        val days = d.daysMissed ?: 0
                        val tickets = d.ticketCount ?: 0
                        d.message ?: "복귀 보상 있음: ${days}일 결석 · 티켓 $tickets"
                    }
                    _uiState.update { it.copy(subtleNotice = msg) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "복귀 보상 조회에 실패했습니다.")
                    }
                }
            )
        }
    }

    fun onClaimReturnReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(subtleNotice = null, errorMessage = null) }
            homeRepository.claimReturnReward().fold(
                onSuccess = { data ->
                    val rem = data.remainingTickets
                    if (rem != null) {
                        applyRemainingTickets(rem.normal, rem.rare, rem.epic)
                    }
                    val extra = data.ticketEarned?.let { te ->
                        val cnt = te.count
                        if (cnt > 0) " (티켓 ${cnt}장)" else null
                    } ?: data.rewards.takeIf { it.isNotEmpty() }?.joinToString { r ->
                        "${r.type} ×${r.count}"
                    }?.let { " ($it)" }
                    _uiState.update {
                        it.copy(subtleNotice = "복귀 보상 수령 완료${extra.orEmpty()}")
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "복귀 보상 수령에 실패했습니다.")
                    }
                }
            )
        }
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, subtleNotice = null) }
            // 홈 경로는 /missions 목록과 맞물리므로, 먼저 미션 캐시를 갱신한 뒤 홈·경로를 구성한다.
            missionRepository.refreshMissions()
            getHomeStatusUseCase().fold(
                onSuccess = { data ->
                    val missions = missionRepository.getMissionsFlow().first()
                    val path = HomePathBuilder.build(data, missions)
                    val notice = computeServerDayNotice(data.serverDate)
                        ?: bootstrapHomeUnavailableNotice()
                    var ui = HomeUiMapper.toUiState(data).copy(
                        pathItems = path,
                        isLoading = false,
                        errorMessage = null,
                        subtleNotice = notice
                    )
                    homeRepository.getEnergy().getOrNull()?.let { energy ->
                        ui = ui.copy(
                            missionStartCost = energy.missionStartCost
                                ?: HomeUiState.DEFAULT_MISSION_START_COST,
                            energyCurrent = energy.energy,
                            energyMax = energy.maxEnergy,
                            nextEnergyRecoverAt = energy.nextEnergyRecoverAt
                                ?: ui.nextEnergyRecoverAt
                        )
                    }
                    walletRepository.getWallet().getOrNull()?.let { wallet ->
                        ui = ui.copy(
                            gearBalance = wallet.gear,
                            heartReviveCost = wallet.heartReviveCost,
                            streakShieldCost = wallet.streakShieldCost
                        )
                    }
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

    /** GET /app/bootstrap — CHILD 이고 homeAvailable=false 일 때 1회성 안내 */
    private fun bootstrapHomeUnavailableNotice(): String? {
        val bootstrap = appBootstrapRepository.lastBootstrap() ?: return null
        if (bootstrap.authType != "CHILD") return null
        if (bootstrap.homeAvailable != false) return null
        return appContext.getString(R.string.home_bootstrap_home_unavailable)
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
