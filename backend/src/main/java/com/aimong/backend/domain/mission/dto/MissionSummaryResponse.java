package com.aimong.backend.domain.mission.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MissionSummaryResponse(
        UUID id,
        int stage,
        String title,
        String description,
        boolean isUnlocked,
        boolean isCompleted,
        LocalDate completedAt,
        boolean isReviewable
) {
}
