package com.aimong.backend.domain.streak.dto;

import java.time.LocalDate;

public record ShieldUseResponse(
        boolean used,
        int shieldCount,
        String status,
        boolean recoveryAvailable,
        LocalDate recoveryDeadlineDate,
        LocalDate lastShieldUsedDate
) {
}
