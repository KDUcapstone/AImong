package com.aimong.backend.domain.customquest.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomQuestStatusResponse(
        UUID questId,
        String status,
        Instant completedAt,
        Instant confirmedAt
) {
}
