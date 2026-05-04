package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.quest.dto.AchievementItemResponse;
import com.aimong.backend.domain.quest.dto.AchievementsResponse;
import com.aimong.backend.domain.quest.dto.ProgressResponse;
import com.aimong.backend.domain.quest.entity.Achievement;
import com.aimong.backend.domain.quest.entity.AchievementType;
import com.aimong.backend.domain.quest.repository.AchievementRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
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
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final ChildProfileRepository childProfileRepository;
    private final MissionAttemptRepository missionAttemptRepository;

    @Transactional
    public AchievementsResponse getAchievements(UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        Map<AchievementType, Achievement> achievements = refreshAchievements(childId, childProfile);
        return new AchievementsResponse(
                orderedTypes().stream()
                        .map(type -> toResponse(achievements.get(type)))
                        .toList()
        );
    }

    @Transactional
    public void unlockByTotalXp(UUID childId, ChildProfile childProfile) {
        refreshAchievements(childId, childProfile);
    }

    private Map<AchievementType, Achievement> refreshAchievements(UUID childId, ChildProfile childProfile) {
        Map<AchievementType, Achievement> achievements = ensureAchievements(childId);
        int totalMissions = Math.toIntExact(missionAttemptRepository.countByChildIdAndReviewFalseAndPassedTrue(childId));
        for (AchievementType type : AchievementType.values()) {
            Achievement achievement = achievements.get(type);
            int currentValue = currentValue(type, childProfile, totalMissions);
            achievement.updateProgress(currentValue);
            if (currentValue >= requiredValue(type) && achievement.isIncomplete()) {
                achievement.complete(KstDateUtils.today());
            }
        }
        return achievements;
    }

    private Map<AchievementType, Achievement> ensureAchievements(UUID childId) {
        Map<AchievementType, Achievement> achievements = new EnumMap<>(AchievementType.class);
        Arrays.stream(AchievementType.values()).forEach(type -> {
            Achievement achievement = achievementRepository.findByChildIdAndAchievementType(childId, type)
                    .orElseGet(() -> achievementRepository.save(Achievement.create(childId, type)));
            achievements.put(type, achievement);
        });
        return achievements;
    }

    private AchievementItemResponse toResponse(Achievement achievement) {
        AchievementType type = achievement.getAchievementType();
        return new AchievementItemResponse(
                type.name(),
                label(type),
                achievement.isCompleted(),
                achievement.getCompletedAt(),
                new ProgressResponse(Math.min(achievement.getCurrentValue(), requiredValue(type)), requiredValue(type))
        );
    }

    private int currentValue(AchievementType type, ChildProfile childProfile, int totalMissions) {
        return switch (type) {
            case MISSION_10, MISSION_30 -> totalMissions;
            case XP_100, XP_500 -> childProfile.getTotalXp();
        };
    }

    private List<AchievementType> orderedTypes() {
        return List.of(AchievementType.MISSION_10, AchievementType.MISSION_30, AchievementType.XP_100, AchievementType.XP_500);
    }

    public static int requiredValue(AchievementType type) {
        return switch (type) {
            case MISSION_10 -> 10;
            case MISSION_30 -> 30;
            case XP_100 -> 100;
            case XP_500 -> 500;
        };
    }

    public static String label(AchievementType type) {
        return switch (type) {
            case MISSION_10 -> "미션 10개 완료";
            case MISSION_30 -> "미션 30개 완료";
            case XP_100 -> "누적 XP 100 획득";
            case XP_500 -> "누적 XP 500 획득";
        };
    }
}
