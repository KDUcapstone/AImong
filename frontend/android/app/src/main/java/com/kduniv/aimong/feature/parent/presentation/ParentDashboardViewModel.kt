package com.kduniv.aimong.feature.parent.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
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

    /** 둘째·셋째 자녀 추가 성공 — 등록 완료 바텀시트(코드·스타터 티켓) 표시용 */
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

    /** 요약·주간·약점 API를 한 번에 갱신한다. */
    private suspend fun refreshAllDashboardForChild(childId: String) {
        val detailResult = parentRepository.getParentChildDetail(childId)
        detailResult.fold(
            onSuccess = { _childDetail.value = it },
            onFailure = { e ->
                _messageEvent.emit(e.message ?: "자녀 상세 조회 실패")
                return
            }
        )
        parentRepository.getChildSummary(childId).fold(
            onSuccess = { _childSummary.value = it },
            onFailure = { _childSummary.value = null }
        )
        parentRepository.getWeeklyStats(childId).fold(
            onSuccess = { _weeklyStats.value = it },
            onFailure = { _weeklyStats.value = null }
        )
        parentRepository.getWeakPoints(childId, page = 0, size = 20).fold(
            onSuccess = { _weakPoints.value = it },
            onFailure = { _weakPoints.value = null }
        )
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
