package com.aimong.backend.domain.streak.dto;

import java.time.LocalDate;
import java.util.UUID;

public record StreakResponse(
        int continuousDays,
        LocalDate lastCompletedDate,
        int todaySetCount,
        int shieldCount,
        PartnerResponse partner
) {

    public record PartnerResponse(
            UUID childId,
            String nickname,
            boolean todayCompleted
    ) {
    }
}
