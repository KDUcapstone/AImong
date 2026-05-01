package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.quest.entity.Achievement;
import com.aimong.backend.domain.quest.entity.AchievementType;
import com.aimong.backend.domain.quest.repository.AchievementRepository;
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
            if (totalXp >= threshold.getValue()
                    && !achievementRepository.existsByChildIdAndAchievementType(childId, threshold.getKey())) {
                achievementRepository.save(Achievement.unlock(childId, threshold.getKey()));
            }
        }
    }

    private Map<AchievementType, Integer> thresholds() {
        Map<AchievementType, Integer> thresholds = new LinkedHashMap<>();
        thresholds.put(AchievementType.SPROUT, 100);
        thresholds.put(AchievementType.EXPLORER, 300);
        thresholds.put(AchievementType.CRITIC, 500);
        thresholds.put(AchievementType.GUARDIAN, 1000);
        return thresholds;
    }
}
