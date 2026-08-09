package com.aimong.backend.domain.stagereward.repository;

import com.aimong.backend.domain.stagereward.entity.StageCompletionReward;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface StageCompletionRewardRepository extends JpaRepository<StageCompletionReward, UUID> {

    Optional<StageCompletionReward> findByChildProfileIdAndStageNumber(UUID childId, int stageNumber);

    List<StageCompletionReward> findAllByChildProfileIdOrderByStageNumberAsc(UUID childId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StageCompletionReward> findWithLockByChildProfileIdAndStageNumber(UUID childId, int stageNumber);
}
