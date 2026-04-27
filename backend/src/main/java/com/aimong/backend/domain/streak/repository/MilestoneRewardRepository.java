package com.aimong.backend.domain.streak.repository;

import com.aimong.backend.domain.streak.entity.MilestoneReward;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRewardRepository extends JpaRepository<MilestoneReward, UUID> {

    boolean existsByChildIdAndMilestoneDays(UUID childId, short milestoneDays);
}
