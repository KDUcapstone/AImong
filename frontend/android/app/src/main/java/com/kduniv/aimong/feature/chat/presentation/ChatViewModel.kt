package com.kduniv.aimong.feature.chat.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.chat.domain.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 사용자 메시지 추가
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(ChatMessage(text, isMine = true))
        _uiState.value = _uiState.value.copy(messages = currentMessages, isLoading = true)

        viewModelScope.launch {
            when (val result = sendChatMessageUseCase(text)) {
                is SendChatMessageUseCase.Result.Success -> {
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    updatedMessages.add(ChatMessage(result.response.reply, isMine = false))
                    _uiState.value = _uiState.value.copy(
                        messages = updatedMessages,
                        isLoading = false
                    )
                }
                is SendChatMessageUseCase.Result.PrivacyViolation -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is SendChatMessageUseCase.Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class ChatMessage(
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
