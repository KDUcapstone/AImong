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

    private static final List<DailyQuestType> BASE_TYPES = List.of(
            DailyQuestType.MISSION_1,
            DailyQuestType.MISSION_3,
            DailyQuestType.XP_20,
            DailyQuestType.CHAT_GPT,
            DailyQuestType.STREAK_CHECK
    );

    private static final List<DailyQuestType> ACTIVE_TYPES = List.of(
            DailyQuestType.MISSION_1,
            DailyQuestType.MISSION_3,
            DailyQuestType.XP_20,
            DailyQuestType.CHAT_GPT,
            DailyQuestType.STREAK_CHECK,
            DailyQuestType.ALL_DAILY
    );

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
        int todayStreakMaintained = todayMissions > 0 ? 1 : 0;

        quests.get(DailyQuestType.MISSION_1)
                .updateProgress(todayMissions, requiredValue(DailyQuestType.MISSION_1), false);
        quests.get(DailyQuestType.MISSION_3)
                .updateProgress(todayMissions, requiredValue(DailyQuestType.MISSION_3), false);
        quests.get(DailyQuestType.XP_20)
                .updateProgress(childProfile.getTodayXp(), requiredValue(DailyQuestType.XP_20), false);
        quests.get(DailyQuestType.CHAT_GPT)
                .updateProgress(todayChats, requiredValue(DailyQuestType.CHAT_GPT), false);
        quests.get(DailyQuestType.STREAK_CHECK)
                .updateProgress(todayStreakMaintained, requiredValue(DailyQuestType.STREAK_CHECK), false);

        int completedBaseQuestCount = (int) BASE_TYPES.stream()
                .filter(type -> quests.get(type).isCompleted())
                .count();
        quests.get(DailyQuestType.ALL_DAILY)
                .updateProgress(completedBaseQuestCount, requiredValue(DailyQuestType.ALL_DAILY), false);
        return quests;
    }

    private Map<DailyQuestType, DailyQuest> ensureDailyQuests(UUID childId, LocalDate today) {
        Map<DailyQuestType, DailyQuest> quests = new EnumMap<>(DailyQuestType.class);
        ACTIVE_TYPES.forEach(type -> {
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
        return ACTIVE_TYPES;
    }

    public static boolean isActiveType(DailyQuestType type) {
        return ACTIVE_TYPES.contains(type);
    }

    public static int activeQuestCount() {
        return ACTIVE_TYPES.size();
    }

    public static int requiredValue(DailyQuestType type) {
        return switch (type) {
            case MISSION_1, CHAT_GPT, STREAK_CHECK -> 1;
            case MISSION_3, ALL_3 -> 3;
            case XP_20 -> 20;
            case ALL_DAILY -> BASE_TYPES.size();
        };
    }

    public static String claimType(DailyQuestType type) {
        return "MANUAL";
    }

    public static String label(DailyQuestType type) {
        return switch (type) {
            case MISSION_1 -> "미션 1개 완료";
            case MISSION_3 -> "미션 3개 완료";
            case XP_20 -> "오늘 XP 20 획득";
            case CHAT_GPT -> "GPT 챗봇과 대화";
            case STREAK_CHECK -> "오늘 스트릭 유지";
            case ALL_DAILY -> "일간 퀘스트 모두 완료";
            case ALL_3 -> "데일리 3개 모두 완료";
        };
    }

    public static String reward(DailyQuestType type) {
        return switch (type) {
            case ALL_DAILY -> "기본 티켓 2장";
            case MISSION_1, MISSION_3, XP_20, CHAT_GPT, STREAK_CHECK, ALL_3 -> "기본 티켓 1장";
        };
    }
}
