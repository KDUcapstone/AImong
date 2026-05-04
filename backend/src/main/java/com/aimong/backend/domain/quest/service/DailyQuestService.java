package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.chat.repository.ChatUsageRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.quest.dto.DailyQuestResponse;
import com.aimong.backend.domain.quest.dto.ProgressResponse;
import com.aimong.backend.domain.quest.dto.QuestItemResponse;
import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import com.aimong.backend.domain.quest.repository.DailyQuestRepository;
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
public class DailyQuestService {

    private final DailyQuestRepository dailyQuestRepository;
    private final ChildProfileRepository childProfileRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final ChatUsageRepository chatUsageRepository;

    @Transactional
    public DailyQuestResponse getDailyQuests(UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        LocalDate today = KstDateUtils.today();
        Map<DailyQuestType, DailyQuest> quests = refreshDailyProgress(childId, childProfile, today);
        return new DailyQuestResponse(
                today,
                childProfile.getTodayXp(),
                orderedTypes().stream()
                        .map(type -> toResponse(quests.get(type)))
                        .toList()
        );
    }

    @Transactional
    public void updateForMissionSuccess(UUID childId, ChildProfile childProfile, LocalDate today) {
        refreshDailyProgress(childId, childProfile, today);
    }

    @Transactional
    public void updateForChatSuccess(UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        refreshDailyProgress(childId, childProfile, KstDateUtils.today());
    }

    public Map<DailyQuestType, DailyQuest> refreshDailyProgress(UUID childId, ChildProfile childProfile, LocalDate today) {
        Map<DailyQuestType, DailyQuest> quests = ensureDailyQuests(childId, today);
        int todayMissions = Math.toIntExact(missionAttemptRepository.countByChildIdAndAttemptDateAndReviewFalseAndPassedTrue(
                childId,
                today
        ));
        int todayChats = chatUsageRepository.findByChildIdAndUsageDate(childId, today)
                .map(usage -> usage.getCount())
                .orElse(0);

        quests.get(DailyQuestType.MISSION_1).updateProgress(todayMissions, requiredValue(DailyQuestType.MISSION_1), true);
        quests.get(DailyQuestType.XP_20).updateProgress(childProfile.getTodayXp(), requiredValue(DailyQuestType.XP_20), false);
        quests.get(DailyQuestType.CHAT_GPT).updateProgress(todayChats, requiredValue(DailyQuestType.CHAT_GPT), true);

        int completedBaseQuestCount = (int) List.of(DailyQuestType.MISSION_1, DailyQuestType.XP_20, DailyQuestType.CHAT_GPT)
                .stream()
                .filter(type -> quests.get(type).isCompleted())
                .count();
        quests.get(DailyQuestType.ALL_3).updateProgress(completedBaseQuestCount, requiredValue(DailyQuestType.ALL_3), false);
        return quests;
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

    private QuestItemResponse toResponse(DailyQuest quest) {
        DailyQuestType type = quest.getQuestType();
        return new QuestItemResponse(
                type.name(),
                label(type),
                reward(type),
                claimType(type),
                quest.isCompleted(),
                quest.isRewardClaimed(),
                new ProgressResponse(Math.min(quest.getCurrentValue(), requiredValue(type)), requiredValue(type))
        );
    }

    private List<DailyQuestType> orderedTypes() {
        return List.of(DailyQuestType.MISSION_1, DailyQuestType.XP_20, DailyQuestType.CHAT_GPT, DailyQuestType.ALL_3);
    }

    public static int requiredValue(DailyQuestType type) {
        return switch (type) {
            case MISSION_1, CHAT_GPT -> 1;
            case XP_20 -> 20;
            case ALL_3 -> 3;
        };
    }

    public static String claimType(DailyQuestType type) {
        return switch (type) {
            case MISSION_1, CHAT_GPT -> "AUTO";
            case XP_20, ALL_3 -> "MANUAL";
        };
    }

    public static String label(DailyQuestType type) {
        return switch (type) {
            case MISSION_1 -> "미션 1개 완료하기";
            case XP_20 -> "오늘 XP 20 획득하기";
            case CHAT_GPT -> "GPT 챗봇과 대화하기";
            case ALL_3 -> "데일리 3개 모두 완료";
        };
    }

    public static String reward(DailyQuestType type) {
        return switch (type) {
            case MISSION_1 -> "자동 적용(별도 수령 없음)";
            case CHAT_GPT -> "XP 5 자동 지급";
            case XP_20, ALL_3 -> "일반 티켓 1장";
        };
    }
}
