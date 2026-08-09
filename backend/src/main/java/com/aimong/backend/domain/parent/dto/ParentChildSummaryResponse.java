package com.aimong.backend.domain.parent.dto;

import java.time.Instant;

public record ParentChildSummaryResponse(
        String nickname,
        String profileImageType,
        int totalXp,
        int continuousDays,
        int shieldCount,
        long weeklyCompletedSetCount,
        long totalCompletedSetCount,
        int currentLevelNo,
        Instant lastActiveAt
) {
}
