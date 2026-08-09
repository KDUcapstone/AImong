package com.aimong.backend.domain.streak.repository;

import com.aimong.backend.domain.streak.entity.StreakMilestone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakMilestoneRepository extends JpaRepository<StreakMilestone, UUID> {

    List<StreakMilestone> findAllByChildIdAndRewardClaimedFalseAndTargetDaysLessThanEqual(
            UUID childId,
            short targetDays
    );
}
