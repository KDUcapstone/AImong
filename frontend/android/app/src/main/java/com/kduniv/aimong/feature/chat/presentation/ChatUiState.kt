package com.kduniv.aimong.feature.chat.presentation

data class ChatPrivacyPrompt(
    val originalText: String,
    val highlightRanges: List<IntRange>,
    /** POST /privacy/event 의 detectedType */
    val detectedType: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    /** 장착 펫 이름 — 말풍선·헤더에 표시 */
    val petDisplayName: String = "에이몽",
    val petStage: String = "GROWTH",
    val petType: String = "",
    val petGrade: String = "NORMAL",
    val petAvatarEmoji: String = "✨",
    val hasEquippedPet: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 개인정보 감지 시 입력창 하이라이트(전체 문자열 기준). */
    val privacyHighlightRanges: List<IntRange> = emptyList(),
    /** 취소 / 가리고 보내기 선택 대기 중이면 non-null. */
    val privacyPrompt: ChatPrivacyPrompt? = null,
    /** 전송 성공 후 입력창 비우기(프래그먼트에서 소비). */
    val pendingInputClear: Boolean = false,
    /** 서버에서 온 오늘 남은 호출 수. null이면 아직 미수신(첫 전송 전). */
    val remainingCalls: Int? = null,
    val inputCharCount: Int = 0
) {
    val sendEnabled: Boolean
        get() = (remainingCalls == null || remainingCalls > 0) && !isLoading

    val status: ChatStatus
        get() = when {
            privacyPrompt != null || privacyHighlightRanges.isNotEmpty() -> ChatStatus.PRIVACY_WARNING
            isLoading -> ChatStatus.WAITING_GPT
            errorMessage != null -> ChatStatus.ERROR
            remainingCalls == 0 -> ChatStatus.LIMIT_REACHED
            else -> ChatStatus.IDLE
        }
}

enum class ChatStatus {
    IDLE, DETECTING, PRIVACY_WARNING, WAITING_GPT, LIMIT_REACHED, OFFLINE, ERROR
}
