package com.aimong.backend.domain.streak.dto;

import java.time.LocalDate;

public record ShieldUseResponse(
        int shieldCount,
        String status,
        int continuousDays,
        LocalDate lastShieldUsedDate,
        boolean recoveryAvailable,
        LocalDate recoveryDeadlineDate
) {
}
