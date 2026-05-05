package com.kduniv.aimong.feature.parent.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.parent.data.ParentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val parentRepository: ParentRepository
) : BaseViewModel() {

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    val children: StateFlow<List<ParentChildItem>> = parentRepository.observeCachedParentChildren()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
}
