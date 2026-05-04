package com.aimong.backend.domain.chat.dto;

public record ChatResponse(
        String reply,
        int remainingCalls,
        String hintSuggestion
) {
}
