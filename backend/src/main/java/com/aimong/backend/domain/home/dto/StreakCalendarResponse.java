package com.aimong.backend.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

public record StreakCalendarResponse(
        String yearMonth,
        int continuousDays,
        List<LocalDate> completedDates,
        List<LocalDate> protectedDates,
        LocalDate recoverableDate,
        List<LocalDate> brokenDates,
        LocalDate today
) {
}
