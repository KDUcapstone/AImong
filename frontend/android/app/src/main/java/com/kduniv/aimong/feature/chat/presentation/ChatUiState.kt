package com.kduniv.aimong.feature.chat.presentation

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 개인정보 차단 시 입력창 하이라이트 구간(전체 문자열 기준). */
    val privacyHighlightRanges: List<IntRange> = emptyList(),
    val privacyWarningMessage: String? = null,
    /** 전송 성공 후 입력창 비우기(프래그먼트에서 소비). */
    val pendingInputClear: Boolean = false,
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
