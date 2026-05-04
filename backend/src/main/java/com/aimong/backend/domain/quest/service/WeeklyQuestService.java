package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.chat.repository.ChatUsageRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.quest.dto.ProgressResponse;
import com.aimong.backend.domain.quest.dto.QuestItemResponse;
import com.aimong.backend.domain.quest.dto.WeeklyQuestResponse;
import com.aimong.backend.domain.quest.entity.WeeklyQuest;
import com.aimong.backend.domain.quest.entity.WeeklyQuestType;
import com.aimong.backend.domain.quest.repository.WeeklyQuestRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeeklyQuestService {

    private final WeeklyQuestRepository weeklyQuestRepository;
    private final ChildProfileRepository childProfileRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final ChatUsageRepository chatUsageRepository;

    @Transactional
    public WeeklyQuestResponse getWeeklyQuests(UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        LocalDate weekStart = KstDateUtils.currentWeekStart();
        Map<WeeklyQuestType, WeeklyQuest> quests = refreshWeeklyProgress(childId, childProfile, weekStart);
        return new WeeklyQuestResponse(
                weekStart,
                childProfile.getWeeklyXp(),
                orderedTypes().stream()
                        .map(type -> toResponse(quests.get(type)))
                        .toList()
        );
    }

    @Transactional
    public void updateForMissionSuccess(UUID childId, ChildProfile childProfile, LocalDate weekStart) {
        refreshWeeklyProgress(childId, childProfile, weekStart);
    }

    @Transactional
    public void updateForChatSuccess(UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        refreshWeeklyProgress(childId, childProfile, KstDateUtils.currentWeekStart());
    }

    public Map<WeeklyQuestType, WeeklyQuest> refreshWeeklyProgress(UUID childId, ChildProfile childProfile, LocalDate weekStart) {
        Map<WeeklyQuestType, WeeklyQuest> quests = ensureWeeklyQuests(childId, weekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        int weeklyMissions = Math.toIntExact(missionAttemptRepository.countByChildIdAndAttemptDateBetweenAndReviewFalseAndPassedTrue(
                childId,
                weekStart,
                weekEnd
        ));
        int weeklyChats = Math.toIntExact(chatUsageRepository.sumCountByChildIdAndUsageDateBetween(childId, weekStart, weekEnd));

        quests.get(WeeklyQuestType.XP_100).updateProgress(childProfile.getWeeklyXp(), requiredValue(WeeklyQuestType.XP_100));
        quests.get(WeeklyQuestType.MISSION_5).updateProgress(weeklyMissions, requiredValue(WeeklyQuestType.MISSION_5));
        quests.get(WeeklyQuestType.CHAT_3).updateProgress(weeklyChats, requiredValue(WeeklyQuestType.CHAT_3));
        return quests;
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

    private QuestItemResponse toResponse(WeeklyQuest quest) {
        WeeklyQuestType type = quest.getQuestType();
        return new QuestItemResponse(
                type.name(),
                label(type),
                reward(type),
                "MANUAL",
                quest.isCompleted(),
                quest.isRewardClaimed(),
                new ProgressResponse(Math.min(quest.getCurrentValue(), requiredValue(type)), requiredValue(type))
        );
    }

    private List<WeeklyQuestType> orderedTypes() {
        return List.of(WeeklyQuestType.XP_100, WeeklyQuestType.MISSION_5, WeeklyQuestType.CHAT_3);
    }

    public static int requiredValue(WeeklyQuestType type) {
        return switch (type) {
            case XP_100 -> 100;
            case MISSION_5 -> 5;
            case CHAT_3 -> 3;
        };
    }

    public static String label(WeeklyQuestType type) {
        return switch (type) {
            case XP_100 -> "이번 주 XP 100 획득하기";
            case MISSION_5 -> "미션 5개 완료하기";
            case CHAT_3 -> "GPT 챗봇 3번 사용하기";
        };
    }

    public static String reward(WeeklyQuestType type) {
        return switch (type) {
            case XP_100 -> "레어 티켓 1장";
            case MISSION_5 -> "일반 티켓 2장";
            case CHAT_3 -> "일반 티켓 1장";
        };
    }
}
