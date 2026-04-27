package com.aimong.backend.domain.streak.repository;

import com.aimong.backend.domain.streak.entity.StreakRecord;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface StreakRecordRepository extends JpaRepository<StreakRecord, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<StreakRecord> findWithLockByChildId(UUID childId);
}
