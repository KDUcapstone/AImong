package com.aimong.backend.domain.parent.dto;

import java.time.Instant;

public record ParentChildSummaryResponse(
        String nickname,
        String profileImageType,
        int totalXp,
        int continuousDays,
        int shieldCount,
        long weeklyMissionCount,
        long totalMissionCount,
        Instant lastActiveAt
) {
}
