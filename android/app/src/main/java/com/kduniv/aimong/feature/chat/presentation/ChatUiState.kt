package com.kduniv.aimong.feature.chat.presentation

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val status: ChatStatus = ChatStatus.IDLE
)

enum class ChatStatus {
    IDLE, DETECTING, PRIVACY_WARNING, WAITING_GPT, LIMIT_REACHED, OFFLINE, ERROR
}
