package com.aimong.backend.domain.parent.dto;

import java.time.LocalDate;

public record ParentDailyProgressStat(
        LocalDate date,
        long missionCount,
        long xpEarned
) {
}
