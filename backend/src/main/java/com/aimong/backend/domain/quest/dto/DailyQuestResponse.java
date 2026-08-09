package com.aimong.backend.domain.quest.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyQuestResponse(
        LocalDate date,
        int todayXp,
        List<QuestItemResponse> quests
) {
}
