package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import com.aimong.backend.domain.quest.repository.DailyQuestRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DailyQuestService {

    private final DailyQuestRepository dailyQuestRepository;

    public void updateForMissionSuccess(UUID childId, ChildProfile childProfile, LocalDate today) {
        Map<DailyQuestType, DailyQuest> quests = ensureDailyQuests(childId, today);

        quests.get(DailyQuestType.MISSION_1).complete(true);

        if (childProfile.getTodayXp() >= 20) {
            quests.get(DailyQuestType.XP_20).complete(false);
        }

        if (quests.get(DailyQuestType.MISSION_1).isCompleted()
                && quests.get(DailyQuestType.XP_20).isCompleted()
                && quests.get(DailyQuestType.CHAT_GPT).isCompleted()) {
            quests.get(DailyQuestType.ALL_3).complete(false);
        }
    }

    private Map<DailyQuestType, DailyQuest> ensureDailyQuests(UUID childId, LocalDate today) {
        Map<DailyQuestType, DailyQuest> quests = new EnumMap<>(DailyQuestType.class);
        Arrays.stream(DailyQuestType.values()).forEach(type -> {
            DailyQuest quest = dailyQuestRepository.findByChildIdAndQuestDateAndQuestType(childId, today, type)
                    .orElseGet(() -> dailyQuestRepository.save(DailyQuest.create(childId, today, type)));
            quests.put(type, quest);
        });
        return quests;
    }
}
