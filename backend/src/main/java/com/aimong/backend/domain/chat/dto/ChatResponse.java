package com.aimong.backend.domain.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        String reply,
        int remainingCalls,
        String hintSuggestion,
        UUID sessionId,
        Instant sessionExpiresAt,
        GeneratedImageResponse image,
        Integer remainingImageCalls
) {
    public ChatResponse(String reply, int remainingCalls, String hintSuggestion) {
        this(reply, remainingCalls, hintSuggestion, null, null, null, null);
    }

    public ChatResponse(String reply, int remainingCalls, String hintSuggestion, UUID sessionId, Instant sessionExpiresAt) {
        this(reply, remainingCalls, hintSuggestion, sessionId, sessionExpiresAt, null, null);
    }

    public record GeneratedImageResponse(
            String b64Json,
            String mimeType,
            String outputFormat,
            String size,
            String quality
    ) {
    }
}
