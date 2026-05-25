package com.aimong.backend.domain.customquest.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomQuestItemResponse(
        UUID questId,
        String title,
        String description,
        String rewardText,
        String status,
        Instant expiresAt,
        Instant completedAt,
        Instant confirmedAt,
        Instant createdAt
) {
}
