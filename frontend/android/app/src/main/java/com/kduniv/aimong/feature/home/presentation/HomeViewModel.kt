package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.home.domain.HomePathBuilder
import com.kduniv.aimong.feature.home.domain.MissionPathDevHelper
import com.kduniv.aimong.feature.mission.data.model.MissionStarLevelDto
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.mission.domain.model.completedDifficultyCount
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
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val missionRepository: MissionRepository,
    private val homeRepository: HomeRepository,
    private val walletRepository: WalletRepository,
    private val appBootstrapRepository: AppBootstrapRepository,
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
                    val rawMissions = missionRepository.getMissionsFlow().first()
                    val missions = MissionPathDevHelper.ensureOnePlayablePerStage(
                        supplementMissionsStarLevels(rawMissions)
                    )
                    val path = HomePathBuilder.build(data, missions)
                    val notice = computeServerDayNotice(data.serverDate)
                        ?: bootstrapHomeUnavailableNotice()
                    var ui = HomeUiMapper.toUiState(data).copy(
                        pathItems = path,
                        missionStarLevelsById = missions.associate { it.missionId to it.starLevels },
                        isLoading = false,
                        errorMessage = null,
                        subtleNotice = notice
                    )
                    supplementEmptyMissionStarLevels(path)
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

    fun missionStarLevels(missionId: String): List<MissionStarLevel> =
        _uiState.value.missionStarLevelsById[missionId].orEmpty()

    /**
     * /missions 목록에 starLevels가 비어 있을 때(2·3스테이지 등) status로 보강.
     * 피커 표시 전 호출하면 잠금 UI가 서버와 맞는다.
     */
    suspend fun ensureMissionStarLevels(missionId: String): List<MissionStarLevel> {
        if (missionId.isBlank()) return emptyList()
        val cached = missionStarLevels(missionId)
        if (cached.isNotEmpty()) return cached
        if (UiMode.useStubNav) return cached
        val fresh = fetchMissionStarLevelsFromStatus(missionId) ?: return cached
        cacheMissionStarLevels(missionId, fresh)
        return fresh
    }

    /** GET /missions 에 starLevels가 비어 있으면 status로 채운 뒤 경로를 구성한다. */
    private suspend fun supplementMissionsStarLevels(missions: List<Mission>): List<Mission> {
        if (UiMode.useStubNav) return missions
        return missions.map { mission ->
            if (mission.starLevels.isNotEmpty()) mission
            else {
                fetchMissionStarLevelsFromStatus(mission.missionId)?.let { stars ->
                    mission.copy(starLevels = stars)
                } ?: mission
            }
        }
    }

    private fun cacheMissionStarLevels(missionId: String, stars: List<MissionStarLevel>) {
        val count = stars.completedDifficultyCount()
        _uiState.update { state ->
            state.copy(
                missionStarLevelsById = state.missionStarLevelsById + (missionId to stars),
                pathItems = state.pathItems.map { item ->
                    patchPathItemStars(item, missionId, count)
                },
            )
        }
    }

    private fun patchPathItemStars(item: HomePathItem, missionId: String, count: Int): HomePathItem =
        when (item) {
            is HomePathItem.TodayStart if item.quizNav.missionId == missionId ->
                item.copy(starsFilled = count)
            is HomePathItem.Start if item.quizNav.missionId == missionId ->
                item.copy(starsFilled = count)
            is HomePathItem.Completed if item.missionId == missionId ->
                item.copy(starsFilled = count)
            is HomePathItem.Review if item.quizNav.missionId == missionId ->
                item.copy(starsFilled = count)
            else -> item
        }

    private suspend fun fetchMissionStarLevelsFromStatus(missionId: String): List<MissionStarLevel>? {
        return try {
            val status = apiService.getMissionStatus(missionId).toResult().getOrThrow()
            mapStarLevelDtos(status.starLevels)
        } catch (_: HttpException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    /** GET /missions 에 starLevels가 비어 있는 미션(2·3스테이지 등)을 status로 보강 */
    private fun supplementEmptyMissionStarLevels(path: List<HomePathItem>) {
        if (UiMode.useStubNav) return
        val ids = path.missionIdsNeedingStarSupplement()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { missionId ->
                if (missionStarLevels(missionId).isNotEmpty()) return@forEach
                fetchMissionStarLevelsFromStatus(missionId)?.let { cacheMissionStarLevels(missionId, it) }
            }
        }
    }

    private fun List<HomePathItem>.missionIdsNeedingStarSupplement(): List<String> =
        mapNotNull { item ->
            when (item) {
                is HomePathItem.TodayStart -> item.quizNav.missionId.takeIf { it.isNotBlank() }
                is HomePathItem.Start -> item.quizNav.missionId.takeIf { it.isNotBlank() }
                is HomePathItem.Completed -> item.missionId.takeIf { it.isNotBlank() }
                is HomePathItem.Review -> item.quizNav.missionId.takeIf { it.isNotBlank() }
                else -> null
            }
        }.distinct()

    private fun mapStarLevelDtos(dtos: List<MissionStarLevelDto>): List<MissionStarLevel> =
        dtos.map {
            MissionStarLevel(
                starLevel = it.starLevel,
                label = it.label,
                totalSetCount = it.totalSetCount,
                completedSetCount = it.completedSetCount,
                isPlayable = it.isPlayable,
                isReviewable = it.isReviewable,
            )
        }

    /** 퀘스트 「미션 학습하기」 — 오늘/다음 시작 가능 미션 + 플레이 가능한 최저 별 단계 */
    fun resolveQuestLearnQuizNav(): HomeQuizNavigation? {
        val items = _uiState.value.pathItems
        val startItem = items.filterIsInstance<HomePathItem.TodayStart>().firstOrNull()
            ?: items.filterIsInstance<HomePathItem.Start>().firstOrNull()
            ?: return null
        val base = when (startItem) {
            is HomePathItem.TodayStart -> startItem.quizNav
            is HomePathItem.Start -> startItem.quizNav
            else -> return null
        }
        return resolveQuizNavWithSelectableStar(base)
    }

    fun resolveQuizNavWithSelectableStar(base: HomeQuizNavigation): HomeQuizNavigation? {
        if (base.entrySetId.isNotBlank()) return base
        if (base.missionId.isBlank()) return null
        val stars = missionStarLevels(base.missionId)
        if (base.starLevel in 1..3) {
            val current = stars.firstOrNull { it.starLevel == base.starLevel }
            if (current?.isPlayable == true) return base
        }
        val next = stars.filter { it.isPlayable }.minByOrNull { it.starLevel }
        return next?.let { base.copy(starLevel = it.starLevel) }
    }

    /**
     * 퀴즈 진입 직전 서버 status로 별 잠금·에너지를 재검증하고, 홈 캐시의 starLevels를 갱신한다.
     */
    suspend fun validateMissionQuizNav(
        nav: HomeQuizNavigation,
        unlockMode: DifficultyUnlockMode,
    ): Result<HomeQuizNavigation> {
        if (nav.missionId.isBlank()) {
            return if (nav.entrySetId.isNotBlank()) {
                Result.success(nav)
            } else {
                Result.failure(Exception(appContext.getString(R.string.mission_no_playable_star_level)))
            }
        }
        if (nav.starLevel !in 1..3) {
            return Result.failure(Exception(appContext.getString(R.string.mission_no_playable_star_level)))
        }
        if (UiMode.useStubNav) {
            return validateMissionQuizNavLocal(nav, unlockMode)
        }
        return try {
            val status = apiService.getMissionStatus(nav.missionId).toResult().getOrThrow()
            val stars = mapStarLevelDtos(status.starLevels)
            cacheMissionStarLevels(nav.missionId, stars)
            if (!status.isUnlocked) {
                return Result.failure(Exception(appContext.getString(R.string.quiz_mission_locked)))
            }
            val energy = status.energy
            if (energy != null && energy.current < energy.required) {
                return Result.failure(Exception(appContext.getString(R.string.quiz_insufficient_energy)))
            }
            val sl = stars.firstOrNull { it.starLevel == nav.starLevel }
            val allowed = when (unlockMode) {
                DifficultyUnlockMode.NEW_PLAY -> sl?.isPlayable == true
                DifficultyUnlockMode.REVIEW -> sl?.isReviewable == true
            }
            if (!allowed) {
                return Result.failure(Exception(appContext.getString(R.string.quiz_star_not_playable)))
            }
            Result.success(nav)
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun validateMissionQuizNavLocal(
        nav: HomeQuizNavigation,
        unlockMode: DifficultyUnlockMode,
    ): Result<HomeQuizNavigation> {
        val stars = missionStarLevels(nav.missionId)
        val sl = stars.firstOrNull { it.starLevel == nav.starLevel }
        val allowed = when {
            stars.isEmpty() -> false
            sl == null -> false
            unlockMode == DifficultyUnlockMode.NEW_PLAY -> sl.isPlayable
            else -> sl.isReviewable
        }
        return if (allowed) Result.success(nav) else {
            Result.failure(Exception(appContext.getString(R.string.quiz_star_not_playable)))
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
