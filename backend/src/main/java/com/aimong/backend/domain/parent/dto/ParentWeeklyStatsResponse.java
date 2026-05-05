package com.aimong.backend.domain.parent.dto;

import java.time.LocalDate;
import java.util.List;

public record ParentWeeklyStatsResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalWeeklyXp,
        int totalWeeklyMissions,
        List<DailyStatResponse> dailyStats
) {

    public record DailyStatResponse(
            LocalDate date,
            String dayOfWeek,
            int missionCount,
            int xpEarned
    ) {
    }
}
