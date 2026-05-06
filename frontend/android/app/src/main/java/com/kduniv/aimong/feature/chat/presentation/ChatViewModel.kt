package com.kduniv.aimong.feature.chat.presentation

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.chat.ChatForegroundTracker
import com.kduniv.aimong.feature.chat.ChatHintNotifier
import com.kduniv.aimong.feature.chat.domain.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val chatForegroundTracker: ChatForegroundTracker,
    private val chatHintNotifier: ChatHintNotifier
) : BaseViewModel() {
    private val messageSeq = AtomicLong(0L)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    fun onInputChanged(length: Int) {
        _uiState.update { it.copy(inputCharCount = length) }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value
        if (!state.sendEnabled) return

        val currentMessages = state.messages.toMutableList()
        currentMessages.add(
            ChatMessage(id = messageSeq.incrementAndGet(), text = text.trim(), isMine = true)
        )
        _uiState.update {
            it.copy(messages = currentMessages, isLoading = true, errorMessage = null)
        }

        viewModelScope.launch {
            when (val result = sendChatMessageUseCase(text)) {
                is SendChatMessageUseCase.Result.Success -> {
                    val r = result.response
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    updatedMessages.add(
                        ChatMessage(id = messageSeq.incrementAndGet(), text = r.reply, isMine = false)
                    )
                    _uiState.update {
                        it.copy(
                            messages = updatedMessages,
                            isLoading = false,
                            remainingCalls = r.remainingCalls
                        )
                    }
                    val hint = r.hintSuggestion?.trim().orEmpty()
                    if (hint.isNotEmpty() && !chatForegroundTracker.isChatVisible) {
                        chatHintNotifier.offerHint(hint)
                    }
                }
                is SendChatMessageUseCase.Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
