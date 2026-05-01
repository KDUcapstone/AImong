package com.aimong.backend.domain.streak.repository;

import com.aimong.backend.domain.streak.entity.StreakRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StreakRecordRepository extends JpaRepository<StreakRecord, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<StreakRecord> findWithLockByChildId(UUID childId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from StreakRecord s
            where s.continuousDays > 0
              and (s.lastCompletedDate is null or s.lastCompletedDate < :yesterday)
            """)
    List<StreakRecord> findMissedActiveStreaksForReset(LocalDate yesterday);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from StreakRecord s
            where s.todayMissionCount > 0
              and (s.lastCompletedDate is null or s.lastCompletedDate < :today)
            """)
    List<StreakRecord> findStaleTodayMissionCounts(LocalDate today);
}
