package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.quest.entity.Achievement;
import com.aimong.backend.domain.quest.entity.AchievementType;
import com.aimong.backend.domain.quest.repository.AchievementRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;

    public void unlockByTotalXp(UUID childId, ChildProfile childProfile) {
        int totalXp = childProfile.getTotalXp();
        for (Map.Entry<AchievementType, Integer> threshold : thresholds().entrySet()) {
            Achievement achievement = achievementRepository.findByChildIdAndAchievementType(childId, threshold.getKey())
                    .orElseGet(() -> achievementRepository.save(Achievement.create(childId, threshold.getKey())));
            achievement.updateProgress(totalXp);
            if (totalXp >= threshold.getValue() && achievement.isIncomplete()) {
                achievement.complete(LocalDate.now());
            }
        }
    }

    private Map<AchievementType, Integer> thresholds() {
        Map<AchievementType, Integer> thresholds = new LinkedHashMap<>();
        thresholds.put(AchievementType.XP_100, 100);
        thresholds.put(AchievementType.XP_500, 500);
        return thresholds;
    }
}
