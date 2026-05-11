package com.kduniv.aimong.feature.parent.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentPrivacyLogResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val parentRepository: ParentRepository
) : BaseViewModel() {

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _selectedChildId = MutableStateFlow<String?>(null)
    val selectedChildId: StateFlow<String?> = _selectedChildId

    private val _summary = MutableStateFlow<ParentChildSummaryResponseData?>(null)
    val summary: StateFlow<ParentChildSummaryResponseData?> = _summary.asStateFlow()

    private val _weeklyStats = MutableStateFlow<ParentWeeklyStatsResponseData?>(null)
    val weeklyStats: StateFlow<ParentWeeklyStatsResponseData?> = _weeklyStats.asStateFlow()

    private val _privacyLog = MutableStateFlow<ParentPrivacyLogResponseData?>(null)
    val privacyLog: StateFlow<ParentPrivacyLogResponseData?> = _privacyLog.asStateFlow()

    private val _weakPoints = MutableStateFlow<ParentWeakPointsResponseData?>(null)
    val weakPoints: StateFlow<ParentWeakPointsResponseData?> = _weakPoints.asStateFlow()

    val children: StateFlow<List<ParentChildItem>> = parentRepository.observeCachedParentChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectChild(childId: String) {
        _selectedChildId.value = childId
        viewModelScope.launch { _messageEvent.emit("선택된 자녀: $childId") }
    }

    fun regenerateChildCode(childId: String) {
        viewModelScope.launch {
            parentRepository.regenerateChildCode(childId).fold(
                onSuccess = { newCode ->
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

    fun fetchSummary() = fetchWithSelectedChild(
        actionName = "요약",
        block = { id ->
            parentRepository.getChildSummary(id).fold(
                onSuccess = { s ->
                    _summary.value = s
                    "요약: XP ${s.totalXp}, 스트릭 ${s.continuousDays}일, 주간완료 ${s.weeklyCompletedSetCount}"
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

    fun fetchPrivacyLog(page: Int = 0, size: Int = 20) = fetchWithSelectedChild(
        actionName = "개인정보로그",
        block = { id ->
            parentRepository.getPrivacyLog(id, page = page, size = size).fold(
                onSuccess = { s ->
                    _privacyLog.value = s
                    "개인정보로그: weekly ${s.weeklyCount}, total ${s.totalCount}, events ${s.events.size}"
                },
                onFailure = { e -> e.message ?: "개인정보로그 조회 실패" }
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
