package com.kduniv.aimong.feature.home.presentation.quest

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
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
    data class ShowToast(val message: String) : QuestSheetEffect
    data class Snackbar(val message: String) : QuestSheetEffect
    data class TicketsPatched(val normal: Int, val rare: Int, val epic: Int) : QuestSheetEffect
}

@HiltViewModel
class QuestListViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _rows = MutableStateFlow<List<QuestSheetRow>>(emptyList())
    val rows: StateFlow<List<QuestSheetRow>> = _rows.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(QuestSheetPeriod.DAILY)
    val selectedPeriod: StateFlow<QuestSheetPeriod> = _selectedPeriod.asStateFlow()

    private val _effects = Channel<QuestSheetEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var canStartMission: Boolean = true

    init {
        loadDaily()
    }

    fun setCanStartMission(value: Boolean) {
        canStartMission = value
    }

    fun selectPeriod(period: QuestSheetPeriod) {
        _selectedPeriod.value = period
        when (period) {
            QuestSheetPeriod.DAILY -> loadDaily()
            QuestSheetPeriod.WEEKLY -> loadWeekly()
        }
    }

    fun loadDaily() {
        viewModelScope.launch {
            _loading.value = true
            questRepository.getDailyQuests().fold(
                onSuccess = { data ->
                    _rows.value = data.quests.map {
                        QuestSheetMapper.mapItem(it, QuestSheetPeriod.DAILY, canStartMission)
                    }
                },
                onFailure = { e ->
                    val mock = com.kduniv.aimong.feature.dev.mock.MockUiSamples.homeUiState().quests
                    _rows.value = mock.map { com.kduniv.aimong.feature.home.presentation.quest.QuestSheetRow(
                        questType = it.id,
                        title = it.title,
                        detailText = it.rewardSummary,
                        period = QuestSheetPeriod.DAILY,
                        primaryAction = if (it.canStart) com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction.GO_LEARN else if (it.isCompleted) com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction.COMPLETED else com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction.IN_PROGRESS,
                        actionEnabled = it.canStart
                    )}
                }
            )
            _loading.value = false
        }
    }

    fun loadWeekly() {
        viewModelScope.launch {
            _loading.value = true
            questRepository.getWeeklyQuests().fold(
                onSuccess = { data ->
                    _rows.value = data.quests.map {
                        QuestSheetMapper.mapItem(it, QuestSheetPeriod.WEEKLY, canStartMission)
                    }
                },
                onFailure = { e ->
                    val mock = com.kduniv.aimong.feature.dev.mock.MockUiSamples.homeUiState().quests
                    _rows.value = mock.map { com.kduniv.aimong.feature.home.presentation.quest.QuestSheetRow(
                        questType = it.id,
                        title = it.title + " (위클리)",
                        detailText = it.rewardSummary,
                        period = QuestSheetPeriod.WEEKLY,
                        primaryAction = if (it.canStart) com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction.GO_LEARN else if (it.isCompleted) com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction.COMPLETED else com.kduniv.aimong.feature.home.presentation.quest.QuestSheetPrimaryAction.IN_PROGRESS,
                        actionEnabled = it.canStart
                    )}
                }
            )
            _loading.value = false
        }
    }

    fun onClaim(questType: String, period: QuestSheetPeriod) {
        val periodStr = when (period) {
            QuestSheetPeriod.DAILY -> "daily"
            QuestSheetPeriod.WEEKLY -> "weekly"
        }
        viewModelScope.launch {
            _loading.value = true
            questRepository.claimQuest(questType, periodStr).fold(
                onSuccess = { data ->
                    val toast = QuestRewardToastFormatter.format(appContext, data.rewards)
                    _effects.trySend(QuestSheetEffect.ShowToast(toast))
                    val t = data.remainingTickets
                    _effects.trySend(QuestSheetEffect.TicketsPatched(t.normal, t.rare, t.epic))
                    when (period) {
                        QuestSheetPeriod.DAILY -> loadDaily()
                        QuestSheetPeriod.WEEKLY -> loadWeekly()
                    }
                },
                onFailure = { e ->
                    _loading.value = false
                    _effects.trySend(QuestSheetEffect.Snackbar(e.message ?: "수령에 실패했습니다."))
                }
            )
        }
    }
}
