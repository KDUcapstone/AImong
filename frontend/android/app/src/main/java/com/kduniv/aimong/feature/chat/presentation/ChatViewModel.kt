package com.kduniv.aimong.feature.chat.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.privacy.PrivacyRadar
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.chat.ChatForegroundTracker
import com.kduniv.aimong.feature.chat.ChatHintNotifier
import com.kduniv.aimong.feature.chat.ChatPetUiHelper
import com.kduniv.aimong.feature.chat.domain.ReportPrivacyEventUseCase
import com.kduniv.aimong.feature.chat.domain.SendChatMessageUseCase
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
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
    private val reportPrivacyEventUseCase: ReportPrivacyEventUseCase,
    private val privacyRadar: PrivacyRadar,
    private val chatForegroundTracker: ChatForegroundTracker,
    private val chatHintNotifier: ChatHintNotifier,
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    @ApplicationContext private val appContext: Context
) : BaseViewModel() {
    private val messageSeq = AtomicLong(0L)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadEquippedPet()
    }

    private fun loadEquippedPet() {
        viewModelScope.launch {
            getHomeStatusUseCase().fold(
                onSuccess = { home ->
                    applyPetContext(
                        pet = home.equippedPet?.let {
                            ChatPetUiHelper.displayName(it.petType, it.grade)
                        } ?: "에이몽",
                        stage = home.equippedPet?.stage ?: "GROWTH"
                    )
                },
                onFailure = {
                    applyPetContext(pet = "에이몽", stage = "GROWTH")
                }
            )
        }
    }

    private fun applyPetContext(pet: String, stage: String) {
        _uiState.update { state ->
            val messages = if (state.messages.isEmpty()) {
                listOf(
                    ChatMessage(
                        id = messageSeq.incrementAndGet(),
                        text = appContext.getString(R.string.chat_welcome_pet_fmt, pet),
                        isMine = false
                    )
                )
            } else {
                state.messages
            }
            state.copy(
                petDisplayName = pet,
                petStage = stage,
                messages = messages
            )
        }
    }

    fun onInputChanged(length: Int) {
        _uiState.update {
            val clearPrivacy =
                it.privacyHighlightRanges.isNotEmpty() || it.privacyPrompt != null
            it.copy(
                inputCharCount = length,
                privacyHighlightRanges = if (clearPrivacy) emptyList() else it.privacyHighlightRanges,
                privacyPrompt = if (clearPrivacy) null else it.privacyPrompt
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
                    privacyPrompt = null,
                    privacyHighlightRanges = emptyList()
                )
            }
            when (val result = sendChatMessageUseCase(text)) {
                is SendChatMessageUseCase.Result.PrivacyBlocked -> {
                    val detectedType = privacyRadar.detectedPrivacyApiType(text)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            privacyHighlightRanges = result.sensitiveRanges,
                            privacyPrompt = ChatPrivacyPrompt(
                                originalText = text,
                                highlightRanges = result.sensitiveRanges,
                                detectedType = detectedType
                            )
                        )
                    }
                }
                is SendChatMessageUseCase.Result.Success -> {
                    val r = result.response
                    val shownMine = result.sentMessage
                    val base = _uiState.value.messages.toMutableList()
                    base.add(ChatMessage(id = messageSeq.incrementAndGet(), text = shownMine, isMine = true))
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

    fun onPrivacySendCancelled() {
        val prompt = _uiState.value.privacyPrompt ?: return
        viewModelScope.launch {
            reportPrivacyEventUseCase(prompt.detectedType, masked = false)
        }
        _uiState.update {
            it.copy(privacyPrompt = null, privacyHighlightRanges = emptyList())
        }
    }

    fun onPrivacyMaskedSend() {
        val prompt = _uiState.value.privacyPrompt ?: return
        val text = prompt.originalText
        val detectedType = prompt.detectedType
        viewModelScope.launch {
            reportPrivacyEventUseCase(detectedType, masked = true)
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    privacyPrompt = null,
                    privacyHighlightRanges = emptyList()
                )
            }
            when (val result = sendChatMessageUseCase.sendMasked(text)) {
                is SendChatMessageUseCase.Result.Success -> {
                    val r = result.response
                    val shownMine = result.sentMessage
                    val base = _uiState.value.messages.toMutableList()
                    base.add(ChatMessage(id = messageSeq.incrementAndGet(), text = shownMine, isMine = true))
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
                is SendChatMessageUseCase.Result.PrivacyBlocked -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "가리고 보내기 처리에 실패했어요. 다시 시도해 주세요.")
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
