package com.kduniv.aimong.feature.parent.presentation

import androidx.lifecycle.viewModelScope
import android.content.Context
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshBus
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshTrigger
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestRequest
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentCustomQuestDto
import com.kduniv.aimong.feature.parent.data.model.ParentStageRewardDto
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val parentRepository: ParentRepository,
    private val parentDashboardRefreshBus: ParentDashboardRefreshBus,
) : BaseViewModel() {

    init {
        viewModelScope.launch {
            parentDashboardRefreshBus.events.collect { trigger ->
                when (trigger) {
                    is ParentDashboardRefreshTrigger.CustomQuestsChanged ->
                        onCustomQuestsChangedExternally(trigger)
                }
            }
        }
    }

    companion object {
        private const val PAST_QUEST_PAGE_SIZE = 10
    }

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _childRegisteredEvent = MutableSharedFlow<ParentRegisterResponse>()
    val childRegisteredEvent = _childRegisteredEvent.asSharedFlow()

    private val _selectedChildId = MutableStateFlow<String?>(null)
    val selectedChildId: StateFlow<String?> = _selectedChildId

    private val _childDetail = MutableStateFlow<ParentChildDetailData?>(null)
    val childDetail: StateFlow<ParentChildDetailData?> = _childDetail.asStateFlow()

    private val _childSummary = MutableStateFlow<ParentChildSummaryResponseData?>(null)
    val childSummary: StateFlow<ParentChildSummaryResponseData?> = _childSummary.asStateFlow()

    private val _weeklyStats = MutableStateFlow<ParentWeeklyStatsResponseData?>(null)
    val weeklyStats: StateFlow<ParentWeeklyStatsResponseData?> = _weeklyStats.asStateFlow()

    private val _weakPoints = MutableStateFlow<ParentWeakPointsResponseData?>(null)
    val weakPoints: StateFlow<ParentWeakPointsResponseData?> = _weakPoints.asStateFlow()

    private val _customQuests = MutableStateFlow<List<ParentCustomQuestDto>>(emptyList())
    val customQuests: StateFlow<List<ParentCustomQuestDto>> = _customQuests.asStateFlow()

    private val _pastCustomQuests = MutableStateFlow<List<ParentCustomQuestDto>>(emptyList())
    val pastCustomQuests: StateFlow<List<ParentCustomQuestDto>> = _pastCustomQuests.asStateFlow()

    private val _pastQuestsExpanded = MutableStateFlow(false)
    val pastQuestsExpanded: StateFlow<Boolean> = _pastQuestsExpanded.asStateFlow()

    private val _pastQuestsHasNext = MutableStateFlow(false)
    val pastQuestsHasNext: StateFlow<Boolean> = _pastQuestsHasNext.asStateFlow()

    private val _pastQuestsLoading = MutableStateFlow(false)
    val pastQuestsLoading: StateFlow<Boolean> = _pastQuestsLoading.asStateFlow()

    private val _dashboardRefreshing = MutableStateFlow(false)
    val dashboardRefreshing: StateFlow<Boolean> = _dashboardRefreshing.asStateFlow()

    private var pastQuestsNextPage = 0

    private val _stageRewards = MutableStateFlow<List<ParentStageRewardDto>>(emptyList())
    val stageRewards: StateFlow<List<ParentStageRewardDto>> = _stageRewards.asStateFlow()

    val children: StateFlow<List<ParentChildItem>> = parentRepository.observeCachedParentChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectChild(childId: String) {
        _selectedChildId.value = childId
        viewModelScope.launch { refreshAllDashboardForChild(childId) }
    }

    /** 화면 재개 시 실세계 미션(승인 대기 포함) 목록만 갱신 */
    fun refreshCustomQuestsOnResume() {
        val childId = _selectedChildId.value ?: return
        if (_childDetail.value?.lastActiveAt.isNullOrBlank()) return
        viewModelScope.launch { refreshCustomQuests(childId) }
    }

    fun refreshSelectedDashboardFromPull() {
        if (_dashboardRefreshing.value) return
        viewModelScope.launch {
            _dashboardRefreshing.value = true
            try {
                val childId = _selectedChildId.value
                if (childId.isNullOrBlank()) {
                    parentRepository.syncParentChildren()
                    return@launch
                }
                refreshAllDashboardForChild(childId)
            } finally {
                _dashboardRefreshing.value = false
            }
        }
    }

    private suspend fun onCustomQuestsChangedExternally(
        trigger: ParentDashboardRefreshTrigger.CustomQuestsChanged,
    ) {
        val eventChildId = trigger.childId?.trim()?.takeIf { it.isNotEmpty() }
        val targetChildId = when {
            eventChildId == null -> _selectedChildId.value
            eventChildId == _selectedChildId.value -> eventChildId
            else -> {
                _selectedChildId.value = eventChildId
                refreshAllDashboardForChild(eventChildId)
                if (trigger.showPendingNotice) {
                    _messageEvent.emit(
                        appContext.getString(R.string.parent_custom_quest_pending_notice),
                    )
                }
                return
            }
        } ?: return
        if (_childDetail.value?.lastActiveAt.isNullOrBlank()) {
            refreshAllDashboardForChild(targetChildId)
            return
        }
        refreshCustomQuests(targetChildId)
        if (trigger.showPendingNotice) {
            _messageEvent.emit(
                appContext.getString(R.string.parent_custom_quest_pending_notice),
            )
        }
    }

    private suspend fun refreshAllDashboardForChild(childId: String) {
        _pastQuestsExpanded.value = false
        _pastCustomQuests.value = emptyList()
        _pastQuestsHasNext.value = false
        pastQuestsNextPage = 0
        val detailResult = parentRepository.getParentChildDetail(childId)
        detailResult.fold(
            onSuccess = { detail ->
                _childDetail.value = detail
                if (detail.lastActiveAt.isNullOrBlank()) {
                    _childSummary.value = null
                    _weeklyStats.value = null
                    _weakPoints.value = null
                    _customQuests.value = emptyList()
                    _pastCustomQuests.value = emptyList()
                    _pastQuestsExpanded.value = false
                    _stageRewards.value = emptyList()
                    return
                }
            },
            onFailure = { e ->
                _messageEvent.emit(e.message ?: "자녀 상세 조회 실패")
                return
            }
        )
        coroutineScope {
            val summaryDeferred = async { parentRepository.getChildSummary(childId) }
            val weeklyDeferred = async { parentRepository.getWeeklyStats(childId) }
            val weakPointsDeferred = async { parentRepository.getWeakPoints(childId, page = 0, size = 20) }
            val customQuestsDeferred = async { parentRepository.getCustomQuests(childId) }
            val stageRewardsDeferred = async { parentRepository.getStageRewards(childId) }

            summaryDeferred.await().fold(
                onSuccess = { _childSummary.value = it },
                onFailure = { _childSummary.value = null },
            )
            weeklyDeferred.await().fold(
                onSuccess = { _weeklyStats.value = it },
                onFailure = { _weeklyStats.value = null },
            )
            weakPointsDeferred.await().fold(
                onSuccess = { _weakPoints.value = it },
                onFailure = { _weakPoints.value = null },
            )
            customQuestsDeferred.await().fold(
                onSuccess = { _customQuests.value = it.quests },
                onFailure = { _customQuests.value = emptyList() },
            )
            stageRewardsDeferred.await().fold(
                onSuccess = { data ->
                    _stageRewards.value = data.stages.sortedBy { it.stageNumber }
                },
                onFailure = { _stageRewards.value = emptyList() },
            )
        }
    }

    fun createCustomQuest(title: String, description: String?, rewardText: String, expiresAt: String) {
        val childId = _selectedChildId.value ?: return
        viewModelScope.launch {
            parentRepository.createCustomQuest(
                childId,
                CreateParentCustomQuestRequest(
                    title = title,
                    description = description?.takeIf { it.isNotBlank() },
                    rewardText = rewardText,
                    expiresAt = expiresAt
                )
            ).fold(
                onSuccess = {
                    refreshCustomQuests(childId)
                    _messageEvent.emit("퀘스트를 만들었어요.")
                },
                onFailure = { e -> _messageEvent.emit(e.message ?: "퀘스트 생성에 실패했습니다.") }
            )
        }
    }

    fun togglePastCustomQuests() {
        val childId = _selectedChildId.value ?: return
        if (_pastQuestsExpanded.value) {
            _pastQuestsExpanded.value = false
            return
        }
        viewModelScope.launch {
            loadPastCustomQuests(childId, reset = true)
            _pastQuestsExpanded.value = true
        }
    }

    fun loadMorePastCustomQuests() {
        val childId = _selectedChildId.value ?: return
        if (_pastQuestsLoading.value || !_pastQuestsHasNext.value) return
        viewModelScope.launch {
            _pastQuestsLoading.value = true
            loadPastCustomQuests(childId, reset = false)
            _pastQuestsLoading.value = false
        }
    }

    fun confirmCustomQuest(questId: String) {
        val childId = _selectedChildId.value ?: return
        viewModelScope.launch {
            parentRepository.confirmCustomQuest(questId).fold(
                onSuccess = {
                    refreshCustomQuests(childId)
                    _messageEvent.emit("퀘스트를 승인했어요.")
                },
                onFailure = { e -> _messageEvent.emit(e.message ?: "승인에 실패했습니다.") }
            )
        }
    }

    fun cancelCustomQuest(questId: String) {
        val childId = _selectedChildId.value ?: return
        viewModelScope.launch {
            parentRepository.cancelCustomQuest(questId).fold(
                onSuccess = {
                    refreshCustomQuests(childId)
                    _messageEvent.emit("퀘스트를 취소했어요.")
                },
                onFailure = { e -> _messageEvent.emit(e.message ?: "취소에 실패했습니다.") }
            )
        }
    }

    fun saveStageReward(stageNumber: Int, rewardText: String, hasExistingReward: Boolean) {
        val childId = _selectedChildId.value ?: return
        viewModelScope.launch {
            parentRepository.saveStageReward(childId, stageNumber, rewardText, hasExistingReward).fold(
                onSuccess = {
                    refreshStageRewards(childId)
                    _messageEvent.emit("단계 보상을 저장했어요.")
                },
                onFailure = { e -> _messageEvent.emit(e.message ?: "보상 저장에 실패했습니다.") }
            )
        }
    }

    private suspend fun refreshCustomQuests(childId: String) {
        parentRepository.getCustomQuests(childId).fold(
            onSuccess = { _customQuests.value = it.quests },
            onFailure = { }
        )
        if (_pastQuestsExpanded.value) {
            loadPastCustomQuests(childId, reset = true)
        }
    }

    private suspend fun loadPastCustomQuests(childId: String, reset: Boolean) {
        if (reset) {
            pastQuestsNextPage = 0
            _pastCustomQuests.value = emptyList()
            _pastQuestsHasNext.value = false
        }
        val page = pastQuestsNextPage
        parentRepository.getCustomQuests(
            childId,
            status = "COMPLETED,CANCELLED",
            page = page,
            size = PAST_QUEST_PAGE_SIZE
        ).fold(
            onSuccess = { data ->
                val batch = data.quests.sortedByDescending { it.pastSortInstant() }
                _pastCustomQuests.value = if (reset) {
                    batch
                } else {
                    (_pastCustomQuests.value + batch)
                        .distinctBy { it.questId }
                        .sortedByDescending { it.pastSortInstant() }
                }
                pastQuestsNextPage = page + 1
                _pastQuestsHasNext.value = data.hasNext ||
                    (data.quests.size >= PAST_QUEST_PAGE_SIZE && data.totalCount > _pastCustomQuests.value.size)
            },
            onFailure = {
                if (reset) _pastCustomQuests.value = emptyList()
                _pastQuestsHasNext.value = false
            }
        )
    }

    private fun ParentCustomQuestDto.pastSortInstant(): Instant {
        val raw = confirmedAt ?: completedAt ?: createdAt
        return raw?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.EPOCH
    }

    private suspend fun refreshStageRewards(childId: String) {
        parentRepository.getStageRewards(childId).fold(
            onSuccess = { _stageRewards.value = it.stages.sortedBy { s -> s.stageNumber } },
            onFailure = { }
        )
    }

    fun regenerateChildCode(childId: String) {
        viewModelScope.launch {
            parentRepository.regenerateChildCode(childId).fold(
                onSuccess = { newCode ->
                    if (_selectedChildId.value == childId) {
                        refreshAllDashboardForChild(childId)
                    }
                    _messageEvent.emit("코드가 성공적으로 재발급되었습니다: $newCode")
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "코드 재발급에 실패했습니다.")
                }
            )
        }
    }

    fun syncChildren() {
        viewModelScope.launch {
            parentRepository.syncParentChildren().fold(
                onSuccess = { list ->
                    _messageEvent.emit("자녀 ${list.size}명 불러옴")
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "자녀 목록 갱신에 실패했습니다.")
                }
            )
        }
    }

    fun updateChildNickname(childId: String, nickname: String) {
        viewModelScope.launch {
            parentRepository.patchParentChild(
                childId,
                PatchParentChildRequest(nickname = nickname.trim())
            ).fold(
                onSuccess = {
                    if (_selectedChildId.value == childId) {
                        refreshAllDashboardForChild(childId)
                    }
                    _messageEvent.emit("닉네임이 변경되었어요.")
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "닉네임 변경에 실패했습니다.")
                }
            )
        }
    }

    fun deleteChild(childId: String) {
        viewModelScope.launch {
            parentRepository.deleteParentChild(childId).fold(
                onSuccess = {
                    val remaining = children.value.filter { it.childId != childId }
                    if (_selectedChildId.value == childId) {
                        _selectedChildId.value = remaining.firstOrNull()?.childId
                        remaining.firstOrNull()?.childId?.let { refreshAllDashboardForChild(it) }
                            ?: run {
                                _childDetail.value = null
                                _childSummary.value = null
                                _customQuests.value = emptyList()
                                _pastCustomQuests.value = emptyList()
                                _pastQuestsExpanded.value = false
                                _stageRewards.value = emptyList()
                            }
                    }
                    _messageEvent.emit("자녀 프로필이 삭제되었어요.")
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "자녀 삭제에 실패했습니다.")
                }
            )
        }
    }

    fun addChild(nickname: String) {
        viewModelScope.launch {
            parentRepository.addParentChild(nickname).fold(
                onSuccess = { r ->
                    _selectedChildId.value = r.childId
                    refreshAllDashboardForChild(r.childId)
                    _childRegisteredEvent.emit(r)
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "자녀 추가에 실패했습니다.")
                }
            )
        }
    }

    fun fetchParentMe() {
        viewModelScope.launch {
            parentRepository.getParentMe().fold(
                onSuccess = { me ->
                    _messageEvent.emit(
                        "parent/me: email=${me.email ?: "-"}, 자녀 ${me.childrenCount ?: 0}명, FCM=${me.hasFcmToken == true}"
                    )
                },
                onFailure = { e ->
                    _messageEvent.emit(e.message ?: "parent/me 조회 실패")
                }
            )
        }
    }

    fun fetchChildDetail() = fetchWithSelectedChild(
        actionName = "자녀상세",
        block = { id ->
            parentRepository.getParentChildDetail(id).fold(
                onSuccess = { d ->
                    _childDetail.value = d
                    val linked = d.lastActiveAt != null
                    if (linked) "연동됨: ${d.nickname} (XP ${d.totalXp})"
                    else "아직 자녀가 코드를 입력하지 않았어요! (코드 ${d.code})"
                },
                onFailure = { e -> e.message ?: "자녀 상세 조회 실패" }
            )
        }
    )

    fun fetchSummary() = fetchWithSelectedChild(
        actionName = "요약",
        block = { id ->
            parentRepository.getChildSummary(id).fold(
                onSuccess = { s ->
                    _childSummary.value = s
                    "요약: ${s.nickname}, XP ${s.totalXp}, 스트릭 ${s.continuousDays}일"
                },
                onFailure = { e -> e.message ?: "요약 조회 실패" }
            )
        }
    )

    fun fetchWeeklyStats() = fetchWithSelectedChild(
        actionName = "주간통계",
        block = { id ->
            parentRepository.getWeeklyStats(id).fold(
                onSuccess = { s ->
                    _weeklyStats.value = s
                    "주간통계: ${s.weekStart}~${s.weekEnd} XP ${s.totalWeeklyXp}, 완료 ${s.totalWeeklyMissions}"
                },
                onFailure = { e -> e.message ?: "주간통계 조회 실패" }
            )
        }
    )

    fun fetchWeakPoints(page: Int = 0, size: Int = 20) = fetchWithSelectedChild(
        actionName = "약점분석",
        block = { id ->
            parentRepository.getWeakPoints(id, page = page, size = size).fold(
                onSuccess = { s ->
                    _weakPoints.value = s
                    val top = s.weakPoints.firstOrNull()
                    if (top == null) "약점분석: 데이터 없음"
                    else "약점: ${top.missionTitle ?: "-"} 오답률 ${top.incorrectRate}, 시도 ${top.attemptCount}"
                },
                onFailure = { e -> e.message ?: "약점분석 조회 실패" }
            )
        }
    )

    private fun fetchWithSelectedChild(actionName: String, block: suspend (String) -> String) {
        viewModelScope.launch {
            val id = _selectedChildId.value
            if (id.isNullOrBlank()) {
                _messageEvent.emit("$actionName: 자녀를 먼저 선택해주세요")
                return@launch
            }
            _messageEvent.emit(block(id))
        }
    }
}
