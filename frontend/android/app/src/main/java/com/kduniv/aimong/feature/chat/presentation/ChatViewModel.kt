package com.kduniv.aimong.feature.chat.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.chat.ChatForegroundTracker
import com.kduniv.aimong.feature.chat.ChatHintNotifier
import com.kduniv.aimong.feature.chat.domain.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val chatHintNotifier: ChatHintNotifier,
    @ApplicationContext private val appContext: Context
) : BaseViewModel() {
    private val messageSeq = AtomicLong(0L)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    fun onInputChanged(length: Int) {
        _uiState.update {
            val clearPrivacy = it.privacyHighlightRanges.isNotEmpty() || it.privacyWarningMessage != null
            it.copy(
                inputCharCount = length,
                privacyHighlightRanges = if (clearPrivacy) emptyList() else it.privacyHighlightRanges,
                privacyWarningMessage = if (clearPrivacy) null else it.privacyWarningMessage
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value
        if (!state.sendEnabled) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    privacyWarningMessage = null,
                    privacyHighlightRanges = emptyList()
                )
            }
            when (val result = sendChatMessageUseCase(text)) {
                is SendChatMessageUseCase.Result.PrivacyBlocked -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            privacyHighlightRanges = result.sensitiveRanges,
                            privacyWarningMessage = appContext.getString(R.string.chat_privacy_blocked_message)
                        )
                    }
                }
                is SendChatMessageUseCase.Result.Success -> {
                    val r = result.response
                    val trimmed = text.trim()
                    val base = _uiState.value.messages.toMutableList()
                    base.add(ChatMessage(id = messageSeq.incrementAndGet(), text = trimmed, isMine = true))
                    base.add(ChatMessage(id = messageSeq.incrementAndGet(), text = r.reply, isMine = false))
                    _uiState.update {
                        it.copy(
                            messages = base,
                            isLoading = false,
                            remainingCalls = r.remainingCalls,
                            pendingInputClear = true
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

    fun clearPrivacyWarning() {
        _uiState.update { it.copy(privacyWarningMessage = null) }
    }

    fun acknowledgeInputClear() {
        _uiState.update { it.copy(pendingInputClear = false) }
    }
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
