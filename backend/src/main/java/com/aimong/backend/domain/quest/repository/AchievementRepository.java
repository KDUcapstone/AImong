package com.aimong.backend.domain.quest.repository;

import com.aimong.backend.domain.quest.entity.Achievement;
import com.aimong.backend.domain.quest.entity.AchievementType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    boolean existsByChildIdAndAchievementType(UUID childId, AchievementType achievementType);

    Optional<Achievement> findByChildIdAndAchievementType(UUID childId, AchievementType achievementType);

    List<Achievement> findAllByChildId(UUID childId);
}
