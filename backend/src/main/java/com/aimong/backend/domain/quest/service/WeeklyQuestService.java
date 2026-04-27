package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.quest.entity.WeeklyQuest;
import com.aimong.backend.domain.quest.entity.WeeklyQuestType;
import com.aimong.backend.domain.quest.repository.WeeklyQuestRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeeklyQuestService {

    private final WeeklyQuestRepository weeklyQuestRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;

    public void updateForMissionSuccess(UUID childId, ChildProfile childProfile, LocalDate weekStart) {
        Map<WeeklyQuestType, WeeklyQuest> quests = ensureWeeklyQuests(childId, weekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        long weeklyMissionCount = missionDailyProgressRepository.countByChildIdAndProgressDateBetween(childId, weekStart, weekEnd);

        if (childProfile.getWeeklyXp() >= 100) {
            quests.get(WeeklyQuestType.XP_100).complete(false);
        }

        if (weeklyMissionCount >= 5) {
            quests.get(WeeklyQuestType.MISSION_5).complete(false);
        }
    }

    private Map<WeeklyQuestType, WeeklyQuest> ensureWeeklyQuests(UUID childId, LocalDate weekStart) {
        Map<WeeklyQuestType, WeeklyQuest> quests = new EnumMap<>(WeeklyQuestType.class);
        Arrays.stream(WeeklyQuestType.values()).forEach(type -> {
            WeeklyQuest quest = weeklyQuestRepository.findByChildIdAndWeekStartAndQuestType(childId, weekStart, type)
                    .orElseGet(() -> weeklyQuestRepository.save(WeeklyQuest.create(childId, weekStart, type)));
            quests.put(type, quest);
        });
        return quests;
    }
}
