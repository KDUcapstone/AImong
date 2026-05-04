package com.aimong.backend.domain.quest.dto;

import java.time.LocalDate;

public record AchievementItemResponse(
        String achievementType,
        String label,
        boolean isCompleted,
        LocalDate completedAt,
        ProgressResponse progress
) {
}
