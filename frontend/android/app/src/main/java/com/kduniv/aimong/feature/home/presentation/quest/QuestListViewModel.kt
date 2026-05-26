package com.kduniv.aimong.feature.home.presentation.quest

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus
import com.kduniv.aimong.feature.home.domain.HomeRefreshTrigger
import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestDto
import com.kduniv.aimong.feature.quest.domain.QuestNotificationHelper
import com.kduniv.aimong.feature.quest.domain.QuestPolicy
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuestSheetEffect {
    data class ShowRewardCelebration(val ui: QuestRewardCelebrationUi) : QuestSheetEffect
    data class Snackbar(val message: String) : QuestSheetEffect
    data class TicketsPatched(val normal: Int) : QuestSheetEffect
}

@HiltViewModel
class QuestListViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val homeRefreshBus: ChildHomeRefreshBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _rows = MutableStateFlow<List<QuestSheetRow>>(emptyList())
    val rows: StateFlow<List<QuestSheetRow>> = _rows.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(QuestSheetPeriod.DAILY)
    val selectedPeriod: StateFlow<QuestSheetPeriod> = _selectedPeriod.asStateFlow()

    private val _emptyMessage = MutableStateFlow<String?>(null)
    val emptyMessage: StateFlow<String?> = _emptyMessage.asStateFlow()

    private val _hasPendingCustomQuest = MutableStateFlow(false)
    val hasPendingCustomQuest: StateFlow<Boolean> = _hasPendingCustomQuest.asStateFlow()

    private val _dailyTabBadgeCount = MutableStateFlow(0)
    val dailyTabBadgeCount: StateFlow<Int> = _dailyTabBadgeCount.asStateFlow()

    private val _weeklyTabBadgeCount = MutableStateFlow(0)
    val weeklyTabBadgeCount: StateFlow<Int> = _weeklyTabBadgeCount.asStateFlow()

    private val _parentTabBadgeCount = MutableStateFlow(0)
    val parentTabBadgeCount: StateFlow<Int> = _parentTabBadgeCount.asStateFlow()

    private val _effects = Channel<QuestSheetEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var canStartMission: Boolean = true
    private var suppressTabBadges: Boolean = false
    private val seenPeriods = mutableSetOf<QuestSheetPeriod>()

    init {
        refreshTabBadges()
        loadDaily()
    }

    /** 이미 퀘스트를 확인한 뒤에는 탭 숫자 배지를 숨깁니다. */
    fun setTabBadgesSuppressed(suppressed: Boolean) {
        suppressTabBadges = suppressed
        if (suppressed) clearTabBadges()
    }

    /** 시트가 열릴 때 일·주·부모 탭 배지를 미리 갱신합니다. */
    fun onSheetOpened() {
        seenPeriods += _selectedPeriod.value
        refreshTabBadges()
    }

    private fun clearTabBadges() {
        _dailyTabBadgeCount.value = 0
        _weeklyTabBadgeCount.value = 0
        _parentTabBadgeCount.value = 0
    }

    private fun publishTabBadges(daily: Int, weekly: Int, parent: Int) {
        if (suppressTabBadges) {
            clearTabBadges()
        } else {
            _dailyTabBadgeCount.value = daily.takeUnless { QuestSheetPeriod.DAILY in seenPeriods } ?: 0
            _weeklyTabBadgeCount.value = weekly.takeUnless { QuestSheetPeriod.WEEKLY in seenPeriods } ?: 0
            _parentTabBadgeCount.value = parent.takeUnless { QuestSheetPeriod.PARENT in seenPeriods } ?: 0
        }
    }

    fun setCanStartMission(value: Boolean) {
        canStartMission = value
    }

    fun selectPeriod(period: QuestSheetPeriod) {
        _selectedPeriod.value = period
        seenPeriods += period
        publishTabBadges(
            _dailyTabBadgeCount.value,
            _weeklyTabBadgeCount.value,
            _parentTabBadgeCount.value,
        )
        when (period) {
            QuestSheetPeriod.DAILY -> loadDaily()
            QuestSheetPeriod.WEEKLY -> loadWeekly()
            QuestSheetPeriod.PARENT -> loadParentCustom()
        }
    }

    fun retry() {
        when (_selectedPeriod.value) {
            QuestSheetPeriod.DAILY -> loadDaily()
            QuestSheetPeriod.WEEKLY -> loadWeekly()
            QuestSheetPeriod.PARENT -> loadParentCustom()
        }
    }

    private fun refreshTabBadges() {
        viewModelScope.launch {
            coroutineScope {
                val dailyDeferred = async { questRepository.getDailyQuests() }
                val weeklyDeferred = async { questRepository.getWeeklyQuests() }
                val parentDeferred = async { questRepository.getChildCustomQuests() }
                val daily = dailyDeferred.await().getOrNull()?.quests
                    ?.let { QuestNotificationHelper.countClaimable(it) } ?: _dailyTabBadgeCount.value
                val weekly = weeklyDeferred.await().getOrNull()?.quests
                    ?.let { QuestNotificationHelper.countClaimable(it) } ?: _weeklyTabBadgeCount.value
                val parentData = parentDeferred.await().getOrNull()
                if (parentData != null) {
                    _hasPendingCustomQuest.value = parentData.hasPendingConfirm
                }
                val parent = parentData?.let {
                    countParentTabNotifications(it.quests)
                } ?: _parentTabBadgeCount.value
                publishTabBadges(daily, weekly, parent)
            }
        }
    }

    private fun countParentTabNotifications(quests: List<ChildCustomQuestDto>): Int =
        quests.count { quest ->
            quest.status.equals("COMPLETED", ignoreCase = true) ||
                quest.status.equals("AUTO_CONFIRMED", ignoreCase = true)
        }

    fun loadDaily() {
        viewModelScope.launch {
            _loading.value = true
            _loadError.value = null
            questRepository.getDailyQuests().fold(
                onSuccess = { data ->
                    publishTabBadges(
                        QuestNotificationHelper.countClaimable(data.quests),
                        _weeklyTabBadgeCount.value,
                        _parentTabBadgeCount.value,
                    )
                    _rows.value = data.quests.map {
                        QuestSheetMapper.mapItem(it, QuestSheetPeriod.DAILY, canStartMission)
                    }
                    _emptyMessage.value = null
                    _loadError.value = null
                },
                onFailure = { e ->
                    _rows.value = emptyList()
                    _loadError.value = e.message?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.quest_load_failed)
                }
            )
            _loading.value = false
        }
    }

    fun loadWeekly() {
        viewModelScope.launch {
            _loading.value = true
            _loadError.value = null
            questRepository.getWeeklyQuests().fold(
                onSuccess = { data ->
                    publishTabBadges(
                        _dailyTabBadgeCount.value,
                        QuestNotificationHelper.countClaimable(data.quests),
                        _parentTabBadgeCount.value,
                    )
                    _rows.value = data.quests.map {
                        QuestSheetMapper.mapItem(it, QuestSheetPeriod.WEEKLY, canStartMission)
                    }
                    _emptyMessage.value = null
                    _loadError.value = null
                },
                onFailure = { e ->
                    _rows.value = emptyList()
                    _loadError.value = e.message?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.quest_load_failed)
                }
            )
            _loading.value = false
        }
    }

    fun onClaim(questType: String, period: QuestSheetPeriod, questTitle: String) {
        if (QuestPolicy.isAutoClaimQuest(questType)) {
            viewModelScope.launch {
                _effects.trySend(
                    QuestSheetEffect.Snackbar(appContext.getString(R.string.quest_auto_no_claim)),
                )
            }
            return
        }
        val periodStr = when (period) {
            QuestSheetPeriod.DAILY -> "daily"
            QuestSheetPeriod.WEEKLY -> "weekly"
            QuestSheetPeriod.PARENT -> return
        }
        viewModelScope.launch {
            _loading.value = true
            questRepository.claimQuest(questType, periodStr).fold(
                onSuccess = { data ->
                    val ticketCount = data.remainingTickets.normal.coerceAtLeast(0)
                    val celebration = QuestRewardCelebrationMapper.from(
                        appContext,
                        questTitle,
                        data.rewards,
                    )
                    _effects.trySend(QuestSheetEffect.ShowRewardCelebration(celebration))
                    _effects.trySend(QuestSheetEffect.TicketsPatched(ticketCount))
                    if (!UiMode.useStubNav) {
                        homeRefreshBus.notify(HomeRefreshTrigger.Full)
                    }
                    when (period) {
                        QuestSheetPeriod.DAILY -> loadDaily()
                        QuestSheetPeriod.WEEKLY -> loadWeekly()
                        QuestSheetPeriod.PARENT -> Unit
                    }
                    refreshTabBadges()
                },
                onFailure = { e ->
                    _loading.value = false
                    _effects.trySend(QuestSheetEffect.Snackbar(e.message ?: "수령에 실패했습니다."))
                }
            )
        }
    }

    fun loadParentCustom() {
        viewModelScope.launch {
            _loading.value = true
            _loadError.value = null
            _emptyMessage.value = null
            questRepository.getChildCustomQuests().fold(
                onSuccess = { data ->
                    _hasPendingCustomQuest.value = data.hasPendingConfirm
                    publishTabBadges(
                        _dailyTabBadgeCount.value,
                        _weeklyTabBadgeCount.value,
                        countParentTabNotifications(data.quests),
                    )
                    _rows.value = data.quests.map {
                        QuestSheetMapper.mapCustomQuest(it, appContext)
                    }
                    if (data.quests.isEmpty()) {
                        _emptyMessage.value = appContext.getString(R.string.child_custom_quest_empty)
                    }
                    _loadError.value = null
                },
                onFailure = { e ->
                    _rows.value = emptyList()
                    _hasPendingCustomQuest.value = false
                    _loadError.value = e.message?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.quest_load_failed)
                },
            )
            _loading.value = false
        }
    }

    fun onCompleteCustomQuest(questId: String) {
        viewModelScope.launch {
            _loading.value = true
            questRepository.completeChildCustomQuest(questId).fold(
                onSuccess = {
                    _effects.trySend(
                        QuestSheetEffect.Snackbar(
                            appContext.getString(R.string.child_custom_quest_complete_success),
                        ),
                    )
                    if (!UiMode.useStubNav) {
                        homeRefreshBus.notify(HomeRefreshTrigger.Full)
                    }
                    loadParentCustom()
                },
                onFailure = { e ->
                    _loading.value = false
                    _effects.trySend(QuestSheetEffect.Snackbar(e.message ?: "완료 요청에 실패했습니다."))
                },
            )
        }
    }

}
