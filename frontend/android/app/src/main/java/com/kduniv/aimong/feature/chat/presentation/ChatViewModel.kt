package com.kduniv.aimong.feature.chat.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.ChatMessageResponse
import com.kduniv.aimong.core.privacy.PrivacyRadar
import com.kduniv.aimong.core.ui.BaseViewModel
import com.kduniv.aimong.feature.chat.ChatForegroundTracker
import com.kduniv.aimong.feature.chat.ChatHintNotifier
import com.kduniv.aimong.feature.chat.domain.ReportPrivacyEventUseCase
import com.kduniv.aimong.feature.chat.domain.SendChatMessageUseCase
import com.kduniv.aimong.feature.gacha.GachaPetCatalog
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.pet.data.model.PetDto
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
    private val petRepository: PetRepository,
    @ApplicationContext private val appContext: Context
) : BaseViewModel() {
    private val messageSeq = AtomicLong(0L)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshEquippedPet()
    }

    /** 수집 탭과 동일 — GET /pet 의 equippedPet 만 사용 */
    fun refreshEquippedPet() {
        viewModelScope.launch {
            petRepository.getPets().fold(
                onSuccess = { data ->
                    val equipped = data.equippedPet
                    if (equipped != null) applyEquippedPet(equipped) else applyNoEquippedPet()
                },
                onFailure = { applyNoEquippedPet() },
            )
        }
    }

    private fun applyEquippedPet(pet: PetDto) {
        applyPetContext(
            petDisplayName = GachaPetCatalog.displayNameFor(pet.petType, pet.grade),
            petType = pet.petType,
            petStage = pet.stage,
            petAvatarEmoji = GachaPetCatalog.emojiFor(pet.petType, pet.grade),
            petGrade = pet.grade,
            hasEquippedPet = true,
        )
    }

    private fun applyNoEquippedPet() {
        applyPetContext(
            petDisplayName = appContext.getString(R.string.chat_default_pet_name),
            petType = "",
            petStage = "GROWTH",
            petAvatarEmoji = "🐾",
            petGrade = "NORMAL",
            hasEquippedPet = false,
        )
    }

    private fun applyPetContext(
        petDisplayName: String,
        petType: String,
        petStage: String,
        petAvatarEmoji: String,
        petGrade: String = "NORMAL",
        hasEquippedPet: Boolean,
    ) {
        _uiState.update { state ->
            val welcomeText = if (hasEquippedPet) {
                appContext.getString(R.string.chat_welcome_pet_fmt, petDisplayName)
            } else {
                appContext.getString(R.string.pet_equip_required_for_xp)
            }
            val messages = when {
                state.messages.isEmpty() -> listOf(newWelcomeMessage(welcomeText))
                shouldRefreshWelcomeOnly(state.messages) ->
                    listOf(newWelcomeMessage(welcomeText))
                else -> state.messages
            }
            state.copy(
                petDisplayName = petDisplayName,
                petType = petType,
                petStage = petStage,
                petGrade = petGrade,
                petAvatarEmoji = petAvatarEmoji,
                hasEquippedPet = hasEquippedPet,
                messages = messages,
            )
        }
    }

    private fun shouldRefreshWelcomeOnly(messages: List<ChatMessage>): Boolean =
        messages.size == 1 && !messages.first().isMine

    private fun newWelcomeMessage(text: String): ChatMessage =
        ChatMessage(
            id = messageSeq.incrementAndGet(),
            text = text,
            isMine = false,
        )

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
            appendOutgoingAndTyping(text.trim())
            when (val result = sendChatMessageUseCase(text)) {
                is SendChatMessageUseCase.Result.PrivacyBlocked -> {
                    val detectedType = privacyRadar.detectedPrivacyApiType(text)
                    _uiState.update {
                        it.copy(
                            messages = removeTypingPlaceholder(it.messages).dropLast(1),
                            isLoading = false,
                            pendingInputClear = false,
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
                    applyAssistantReply(result.response, result.sentMessage)
                }
                is SendChatMessageUseCase.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            messages = removeTypingPlaceholder(it.messages),
                            isLoading = false,
                            pendingInputClear = false,
                            errorMessage = result.message,
                        )
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
            val (maskedPreview, _) = privacyRadar.maskForChatSend(text)
            val displayOutgoing = maskedPreview.trim()
            if (displayOutgoing.isEmpty()) {
                _uiState.update {
                    it.copy(errorMessage = "보낼 내용이 없어요. 문장을 조금 더 적어 주세요.")
                }
                return@launch
            }
            appendOutgoingAndTyping(displayOutgoing)
            when (val result = sendChatMessageUseCase.sendMasked(text)) {
                is SendChatMessageUseCase.Result.Success -> {
                    applyAssistantReply(result.response, result.sentMessage)
                }
                is SendChatMessageUseCase.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            messages = removeTypingPlaceholder(it.messages),
                            isLoading = false,
                            pendingInputClear = false,
                            errorMessage = result.message,
                        )
                    }
                }
                is SendChatMessageUseCase.Result.PrivacyBlocked -> {
                    _uiState.update {
                        it.copy(
                            messages = removeTypingPlaceholder(it.messages).dropLast(1),
                            isLoading = false,
                            pendingInputClear = false,
                            errorMessage = "가리고 보내기 처리에 실패했어요. 다시 시도해 주세요.",
                        )
                    }
                }
            }
        }
    }

    private fun appendOutgoingAndTyping(outgoingText: String) {
        val user = ChatMessage(
            id = messageSeq.incrementAndGet(),
            text = outgoingText,
            isMine = true,
        )
        val typing = ChatMessage(
            id = messageSeq.incrementAndGet(),
            text = "",
            isMine = false,
            isTyping = true,
        )
        _uiState.update {
            it.copy(
                messages = it.messages + user + typing,
                isLoading = true,
                pendingInputClear = true,
                errorMessage = null,
                privacyPrompt = null,
                privacyHighlightRanges = emptyList(),
            )
        }
    }

    private fun applyAssistantReply(response: ChatMessageResponse, sentMessage: String) {
        val r = response
        _uiState.update { state ->
            val withoutTyping = removeTypingPlaceholder(state.messages)
            val last = withoutTyping.lastOrNull()
            val withUser = if (last?.isMine == true && last.text == sentMessage) {
                withoutTyping
            } else if (last?.isMine == true) {
                withoutTyping.dropLast(1) + last.copy(text = sentMessage)
            } else {
                withoutTyping + ChatMessage(
                    id = messageSeq.incrementAndGet(),
                    text = sentMessage,
                    isMine = true,
                )
            }
            state.copy(
                messages = withUser + ChatMessage(
                    id = messageSeq.incrementAndGet(),
                    text = r.reply,
                    isMine = false,
                ),
                isLoading = false,
                remainingCalls = r.remainingCalls,
                pendingInputClear = true,
            )
        }
        val hint = r.hintSuggestion?.trim().orEmpty()
        if (hint.isNotEmpty() && !chatForegroundTracker.isChatVisible) {
            chatHintNotifier.offerHint(hint)
        }
    }

    private fun removeTypingPlaceholder(messages: List<ChatMessage>): List<ChatMessage> =
        messages.filterNot { it.isTyping }

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
    val isTyping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
