package com.kduniv.aimong.feature.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.home.domain.HomeRefreshTrigger
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.home.domain.HomePathBuilder
import com.kduniv.aimong.feature.home.domain.MissionPathDevHelper
import com.kduniv.aimong.feature.home.presentation.resolveUnlockModeForPick
import com.kduniv.aimong.feature.mission.data.MissionStatusCache
import com.kduniv.aimong.feature.mission.data.model.MissionStarLevelDto
import com.kduniv.aimong.feature.mission.data.model.MissionStatusResponseData
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.mission.domain.model.mergePreservingHigherUnlock
import com.kduniv.aimong.feature.mission.domain.model.normalizeToThreeLevels
import com.kduniv.aimong.feature.mission.domain.model.openDifficultyCount
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.kduniv.aimong.feature.mission.domain.model.needsStatusStarSupplement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
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
    private val questRepository: QuestRepository,
    private val appBootstrapRepository: AppBootstrapRepository,
    private val apiService: AimongApiService,
    private val missionStatusCache: MissionStatusCache,
    private val homeRefreshBus: ChildHomeRefreshBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _pendingAimongCelebration = MutableStateFlow<AimongCelebrationUi?>(null)
    val pendingAimongCelebration: StateFlow<AimongCelebrationUi?> = _pendingAimongCelebration.asStateFlow()

    private val homePrefs by lazy {
        appContext.getSharedPreferences(PREFS_HOME, Context.MODE_PRIVATE)
    }

    private var homeLoadJob: Job? = null

    init {
        if (!UiMode.useStubNav) {
            viewModelScope.launch {
                homeRefreshBus.events
                    .debounce(200)
                    .collect { handleRefreshTrigger(it) }
            }
        }
    }

    fun onHomeResumed() {
        loadHome(showLoading = _uiState.value.pathItems.isEmpty())
    }

    private fun handleRefreshTrigger(trigger: HomeRefreshTrigger) {
        when (trigger) {
            is HomeRefreshTrigger.TicketsUpdated -> patchTicketCount(trigger.normal)
            is HomeRefreshTrigger.MissionCompleted -> {
                missionStatusCache.clear()
                applyMissionXpHint(trigger.xpEarned, trigger.equippedPetXp)
                loadHome(showLoading = false)
            }
            is HomeRefreshTrigger.PetAimongAchieved -> {
                _pendingAimongCelebration.value = AimongCelebrationUi(
                    petName = trigger.petName,
                    petType = trigger.petType,
                    grade = trigger.grade,
                )
                loadHome(showLoading = false)
            }
            HomeRefreshTrigger.Full -> {
                missionStatusCache.clear()
                loadHome(showLoading = false)
            }
        }
    }

    fun consumeAimongCelebration() {
        _pendingAimongCelebration.value = null
    }

    private fun applyMissionXpHint(xpEarned: Int, equippedPetXp: Int) {
        if (xpEarned <= 0 && equippedPetXp <= 0) return
        _uiState.update { s ->
            val userXp = if (xpEarned > 0) s.topStatusXp + xpEarned else s.topStatusXp
            val petXp = when {
                equippedPetXp > 0 -> equippedPetXp
                xpEarned > 0 && s.hasEquippedPet && s.showPetXpProgress -> s.petXp + xpEarned
                else -> s.petXp
            }
            s.copy(
                topStatusXp = userXp,
                totalXp = userXp,
                userLevel = 1 + (userXp / 80).coerceIn(0, 99),
                petXp = petXp,
            )
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSubtleNotice() {
        _uiState.update { it.copy(subtleNotice = null) }
    }

    /** 퀘스트·복귀 등 — 홈 칩 즉시 반영 + 수집 탭 알림 */
    fun applyRemainingTickets(normal: Int) {
        val count = normal.coerceAtLeast(0)
        patchTicketCount(count)
        homeRefreshBus.notify(HomeRefreshTrigger.TicketsUpdated(count))
    }

    private fun patchTicketCount(normal: Int) {
        val count = normal.coerceAtLeast(0)
        _uiState.update { s ->
            s.copy(
                normalTickets = count,
                topTicketCount = count,
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

    /** 퀘스트 바텀시트를 열면 현재 알림을 확인한 것으로 처리합니다. */
    fun acknowledgeQuestNotifications() {
        _uiState.update { it.copy(questNotificationsAcknowledged = true) }
    }

    fun onClaimReturnReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(subtleNotice = null, errorMessage = null) }
            homeRepository.claimReturnReward().fold(
                onSuccess = { data ->
                    val rem = data.remainingTickets
                    if (rem != null) {
                        applyRemainingTickets(rem.normal)
                    }
                    if (!UiMode.useStubNav) {
                        homeRefreshBus.notify(HomeRefreshTrigger.Full)
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

    private suspend fun resolveStageRewardsForPath(): Map<Int, StageRewardUi> {
        val fromApi = homeRepository.getStageRewards().getOrNull()?.stages
            ?.map { StageRewardUi.fromDto(it) }
            ?.associateBy { it.stageNumber }
        if (!fromApi.isNullOrEmpty()) return fromApi
        return StageRewardUi.defaultsForStages(listOf(1, 2))
    }

    private fun loadHome(showLoading: Boolean = true) {
        homeLoadJob?.cancel()
        homeLoadJob = viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, subtleNotice = null) }
            }
            // 홈 경로는 /missions 목록과 맞물리므로, 먼저 미션 캐시를 갱신한 뒤 홈·경로를 구성한다.
            val missionsRefresh = missionRepository.refreshMissions()
            getHomeStatusUseCase().fold(
                onSuccess = { data ->
                    val rawMissions = missionRepository.getMissionsFlow().first()
                    if (rawMissions.isEmpty() && missionsRefresh.isFailure) {
                        val refreshError = missionsRefresh.exceptionOrNull()
                        if (refreshError is CancellationException) return@launch
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = refreshError?.message
                                    ?: appContext.getString(R.string.home_missions_refresh_failed),
                            )
                        }
                        return@launch
                    }
                    val missions = MissionPathDevHelper.applyPathUnlockGuarantees(
                        supplementMissionsStarLevels(rawMissions)
                    )
                    val stageRewards = resolveStageRewardsForPath()
                    val path = HomePathBuilder.build(data, missions, stageRewards)
                    val missionsRefreshNotice = if (missionsRefresh.isFailure && rawMissions.isNotEmpty()) {
                        val refreshError = missionsRefresh.exceptionOrNull()
                        if (refreshError is CancellationException) {
                            null
                        } else {
                            refreshError?.message
                                ?: appContext.getString(R.string.home_missions_refresh_failed)
                        }
                    } else {
                        null
                    }
                    val notice = computeServerDayNotice(data.serverDate)
                        ?: missionsRefreshNotice
                        ?: bootstrapHomeUnavailableNotice()
                    var ui = HomeUiMapper.toUiState(data).copy(
                        pathItems = path,
                        missionStarLevelsById = missions.associate { it.missionId to it.starLevels },
                        missionUnlockedById = missions.associate { it.missionId to it.isUnlocked },
                        isLoading = false,
                        errorMessage = null,
                        subtleNotice = notice
                    )
                    supplementEmptyMissionStarLevels(path)
                    coroutineScope {
                        val energyDeferred = async { homeRepository.getEnergy() }
                        val walletDeferred = async { walletRepository.getWallet() }
                        energyDeferred.await().getOrNull()?.let { energy ->
                            ui = ui.copy(
                                missionStartCost = energy.missionStartCost
                                    ?: HomeUiState.DEFAULT_MISSION_START_COST,
                                energyCurrent = energy.energy,
                                energyMax = energy.maxEnergy,
                                nextEnergyRecoverAt = energy.nextEnergyRecoverAt
                                    ?: ui.nextEnergyRecoverAt,
                            )
                        }
                        walletDeferred.await().getOrNull()?.let { wallet ->
                            ui = ui.copy(
                                gearBalance = wallet.gear,
                                heartReviveCost = wallet.heartReviveCost,
                                streakShieldCost = wallet.streakShieldCost,
                            )
                        }
                    }
                    questRepository.getChildCustomQuests().getOrNull()?.let { custom ->
                        ui = ui.copy(hasPendingCustomQuest = custom.hasPendingConfirm)
                    }
                    if (ui.questNotificationCount() == 0) {
                        ui = ui.copy(questNotificationsAcknowledged = false)
                    }
                    _uiState.value = ui
                    homeRefreshBus.notify(HomeRefreshTrigger.TicketsUpdated(ui.normalTickets))
                },
                onFailure = { e ->
                    if (e is CancellationException) return@launch
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
        if (UiMode.useStubNav) return missionStarLevels(missionId).normalizeToThreeLevels()

        val cached = missionStarLevels(missionId).normalizeToThreeLevels()
        val snapshot = fetchMissionStatusSnapshot(missionId)
        val merged = snapshot?.stars?.let { statusStars ->
            cached.mergePreservingHigherUnlock(statusStars)
        } ?: cached

        val resolved = when {
            merged.openDifficultyCount() > 0 -> merged
            isMissionUnlocked(missionId) -> defaultPlayableStarLevels()
            else -> merged
        }

        cacheMissionStatus(
            missionId = missionId,
            stars = resolved,
            isUnlocked = snapshot?.isUnlocked ?: isMissionUnlocked(missionId),
        )
        return resolved
    }

    private fun defaultPlayableStarLevels(): List<MissionStarLevel> = listOf(
        MissionStarLevel(1, "쉬움", 2, 0, isPlayable = true, isReviewable = false),
        MissionStarLevel(2, "보통", 2, 0, isPlayable = false, isReviewable = false),
        MissionStarLevel(3, "어려움", 2, 0, isPlayable = false, isReviewable = false),
    )

    fun isMissionUnlocked(missionId: String): Boolean =
        _uiState.value.isMissionUnlocked(missionId)

    /**
     * GET /missions 의 starLevels 로 경로를 먼저 구성한다.
     * status 는 화면에 보이는 미션만 [supplementEmptyMissionStarLevels] 에서 지연·병렬 보강.
     */
    private fun supplementMissionsStarLevels(missions: List<Mission>): List<Mission> {
        if (UiMode.useStubNav) return missions
        return missions.map { mission ->
            val base = mission.copy(starLevels = mission.starLevels.normalizeToThreeLevels())
            val stars = when {
                !base.needsStatusStarSupplement() -> base.starLevels
                base.isUnlocked && base.stage == 1 ->
                    MissionPathDevHelper.withGuaranteedEasyPlayable(base).starLevels.normalizeToThreeLevels()
                else -> base.starLevels
            }
            base.copy(starLevels = stars)
        }
    }

    private fun cacheMissionStatus(
        missionId: String,
        stars: List<MissionStarLevel>,
        isUnlocked: Boolean? = null,
    ) {
        val count = stars.openDifficultyCount()
        _uiState.update { state ->
            state.copy(
                missionStarLevelsById = state.missionStarLevelsById + (missionId to stars),
                missionUnlockedById = if (isUnlocked != null) {
                    state.missionUnlockedById + (missionId to isUnlocked)
                } else {
                    state.missionUnlockedById
                },
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

    private data class MissionStatusSnapshot(
        val stars: List<MissionStarLevel>,
        val isUnlocked: Boolean,
    )

    private suspend fun getMissionStatus(
        missionId: String,
        forceNetwork: Boolean = false,
    ): MissionStatusResponseData? {
        if (missionId.isBlank()) return null
        if (!forceNetwork) {
            missionStatusCache.get(missionId)?.let { return it }
        }
        return try {
            val status = apiService.getMissionStatus(missionId).toResult().getOrThrow()
            missionStatusCache.put(missionId, status)
            status
        } catch (_: HttpException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun fetchMissionStatusSnapshot(missionId: String): MissionStatusSnapshot? {
        val status = getMissionStatus(missionId) ?: return null
        return MissionStatusSnapshot(
            stars = mapStarLevelDtos(status.starLevels),
            isUnlocked = status.isUnlocked,
        )
    }

    /** 경로에 노출된 미션 중 목록 API 만으로 부족한 것만 status 로 보강(비동기·병렬) */
    private fun supplementEmptyMissionStarLevels(path: List<HomePathItem>) {
        if (UiMode.useStubNav) return
        val ids = path.missionIdsNeedingStarSupplement().filter { missionId ->
            val mission = _uiState.value.missionStarLevelsById[missionId]
            mission == null || mission.none { it.isPlayable || it.isReviewable || it.isCompleted }
        }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val limiter = Semaphore(MAX_PARALLEL_MISSION_STATUS)
            supervisorScope {
                ids.map { missionId ->
                    async {
                        limiter.withPermit { ensureMissionStarLevels(missionId) }
                    }
                }.awaitAll()
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
        }.normalizeToThreeLevels()

    /** 퀘스트 「미션 학습하기」 — 오늘/다음 시작 가능 미션 + 별 단계·진입 모드 */
    fun resolveQuestLearnEntry(): Pair<HomeQuizNavigation, DifficultyUnlockMode>? {
        val items = _uiState.value.pathItems
        val today = items.filterIsInstance<HomePathItem.TodayStart>().firstOrNull()
        if (today != null) {
            return today.quizNav to today.unlockMode
        }
        val startItem = items.filterIsInstance<HomePathItem.Start>().firstOrNull() ?: return null
        val nav = resolveQuizNavWithSelectableStar(startItem.quizNav) ?: return null
        val mode = missionStarLevels(nav.missionId)
            .firstOrNull { it.starLevel == nav.starLevel }
            ?.resolveUnlockModeForPick()
            ?: DifficultyUnlockMode.NEW_PLAY
        return nav to mode
    }

    fun resolveQuizNavWithSelectableStar(base: HomeQuizNavigation): HomeQuizNavigation? {
        if (base.entrySetId.isNotBlank()) return base
        if (base.missionId.isBlank()) return null
        val stars = missionStarLevels(base.missionId)
        if (base.starLevel in 1..3) {
            val current = stars.firstOrNull { it.starLevel == base.starLevel }
            if (current?.isPlayable == true || current?.isReviewOnly == true) return base
        }
        val nextPlayable = stars.filter { it.isPlayable }.minByOrNull { it.starLevel }
        if (nextPlayable != null) {
            return base.copy(starLevel = nextPlayable.starLevel)
        }
        val nextReview = stars.filter { it.isReviewOnly }.minByOrNull { it.starLevel }
        return nextReview?.let { base.copy(starLevel = it.starLevel) }
    }

    /**
     * 퀴즈 진입 직전 서버 status로 별 잠금·에너지를 재검증하고, 홈 캐시의 starLevels를 갱신한다.
     */
    suspend fun validateMissionQuizNav(
        nav: HomeQuizNavigation,
        unlockMode: DifficultyUnlockMode,
    ): Result<HomeQuizNavigation> {
        if (nav.entrySetId.isNotBlank()) {
            return validateEntrySetQuizNav(nav, unlockMode)
        }
        if (nav.missionId.isBlank()) {
            return Result.failure(Exception(appContext.getString(R.string.mission_no_playable_star_level)))
        }
        if (nav.starLevel !in 1..3) {
            return Result.failure(Exception(appContext.getString(R.string.mission_no_playable_star_level)))
        }
        if (UiMode.useStubNav) {
            return validateMissionQuizNavLocal(nav, unlockMode)
        }
        return try {
            val status = getMissionStatus(nav.missionId)
                ?: return Result.failure(Exception(appContext.getString(R.string.home_missions_refresh_failed)))
            val stars = missionStarLevels(nav.missionId)
                .mergePreservingHigherUnlock(mapStarLevelDtos(status.starLevels))
            cacheMissionStatus(nav.missionId, stars, status.isUnlocked)
            if (!status.isUnlocked) {
                return Result.failure(Exception(appContext.getString(R.string.quiz_mission_locked)))
            }
            if (unlockMode != DifficultyUnlockMode.REVIEW) {
                val energy = status.energy
                if (energy != null && energy.current < energy.required) {
                    return Result.failure(Exception(appContext.getString(R.string.quiz_insufficient_energy)))
                }
            }
            val sl = stars.firstOrNull { it.starLevel == nav.starLevel }
            val allowed = when (unlockMode) {
                DifficultyUnlockMode.NEW_PLAY -> sl?.isPlayable == true
                DifficultyUnlockMode.REVIEW -> sl?.isReviewOnly == true
                DifficultyUnlockMode.PER_STAR -> sl?.let { it.isPlayable || it.isReviewOnly } == true
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

    private suspend fun validateEntrySetQuizNav(
        nav: HomeQuizNavigation,
        unlockMode: DifficultyUnlockMode,
    ): Result<HomeQuizNavigation> {
        if (UiMode.useStubNav) return Result.success(nav)
        if (nav.missionId.isBlank()) return Result.success(nav)
        return try {
            val status = getMissionStatus(nav.missionId)
                ?: return Result.failure(Exception(appContext.getString(R.string.home_missions_refresh_failed)))
            cacheMissionStatus(nav.missionId, mapStarLevelDtos(status.starLevels), status.isUnlocked)
            if (!status.isUnlocked) {
                return Result.failure(Exception(appContext.getString(R.string.quiz_mission_locked)))
            }
            if (unlockMode != DifficultyUnlockMode.REVIEW) {
                val energy = status.energy
                if (energy != null && energy.current < energy.required) {
                    return Result.failure(Exception(appContext.getString(R.string.quiz_insufficient_energy)))
                }
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
        if (nav.entrySetId.isNotBlank()) return Result.success(nav)
        val stars = missionStarLevels(nav.missionId)
        val sl = stars.firstOrNull { it.starLevel == nav.starLevel }
        val allowed = when {
            stars.isEmpty() -> false
            sl == null -> false
            unlockMode == DifficultyUnlockMode.NEW_PLAY -> sl.isPlayable
            unlockMode == DifficultyUnlockMode.REVIEW -> sl.isReviewOnly
            else -> sl.isPlayable || sl.isReviewOnly
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
        /** 홈 진입 시 status fan-out 상한 — 미션 수 × p50 지연 방지 */
        private const val MAX_PARALLEL_MISSION_STATUS = 3
    }
}
