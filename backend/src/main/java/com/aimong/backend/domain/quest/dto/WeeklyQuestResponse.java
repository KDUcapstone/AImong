package com.aimong.backend.domain.quest.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyQuestResponse(
        LocalDate weekStart,
        int weeklyXp,
        List<QuestItemResponse> quests
) {
}
