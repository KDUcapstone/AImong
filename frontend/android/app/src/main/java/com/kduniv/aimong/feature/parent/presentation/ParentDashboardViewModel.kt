package com.kduniv.aimong.feature.parent.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentChildDetailResponseData
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.parent.data.ParentRepository
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

    private val _childDetail = MutableStateFlow<ParentChildDetailResponseData?>(null)
    val childDetail: StateFlow<ParentChildDetailResponseData?> = _childDetail.asStateFlow()

    val children: StateFlow<List<ParentChildItem>> = parentRepository.observeCachedParentChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectChild(childId: String) {
        _selectedChildId.value = childId
        viewModelScope.launch {
            _messageEvent.emit("선택된 자녀: $childId")
            fetchChildDetail()
        }
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
