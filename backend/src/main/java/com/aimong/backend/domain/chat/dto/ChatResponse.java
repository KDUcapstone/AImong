package com.aimong.backend.domain.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        String reply,
        int remainingCalls,
        String hintSuggestion,
        UUID sessionId,
        Instant sessionExpiresAt
) {
    public ChatResponse(String reply, int remainingCalls, String hintSuggestion) {
        this(reply, remainingCalls, hintSuggestion, null, null);
    }
}
