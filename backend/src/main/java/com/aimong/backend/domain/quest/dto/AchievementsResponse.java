package com.aimong.backend.domain.quest.dto;

import java.util.List;

public record AchievementsResponse(
        List<AchievementItemResponse> achievements
) {
}
