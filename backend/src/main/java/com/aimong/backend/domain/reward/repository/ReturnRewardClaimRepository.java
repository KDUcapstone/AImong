package com.aimong.backend.domain.reward.repository;

import com.aimong.backend.domain.reward.entity.ReturnRewardClaim;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRewardClaimRepository extends JpaRepository<ReturnRewardClaim, UUID> {

    Optional<ReturnRewardClaim> findByChildIdAndBaseLastCompletedDate(UUID childId, LocalDate baseLastCompletedDate);
}
