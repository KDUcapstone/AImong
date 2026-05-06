package com.kduniv.aimong.feature.chat.presentation

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 서버에서 온 오늘 남은 호출 수. null이면 아직 미수신(첫 전송 전). */
    val remainingCalls: Int? = null,
    val inputCharCount: Int = 0
) {
    val sendEnabled: Boolean
        get() = (remainingCalls == null || remainingCalls > 0) && !isLoading
}

enum class ChatStatus {
    IDLE, DETECTING, PRIVACY_WARNING, WAITING_GPT, LIMIT_REACHED, OFFLINE, ERROR
}
