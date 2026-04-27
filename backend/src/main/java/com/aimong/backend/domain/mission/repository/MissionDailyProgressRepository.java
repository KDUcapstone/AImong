package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress.MissionDailyProgressId;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MissionDailyProgressRepository extends JpaRepository<MissionDailyProgress, MissionDailyProgressId> {

    Optional<MissionDailyProgress> findByChildIdAndMissionIdAndProgressDate(UUID childId, UUID missionId, LocalDate progressDate);

    long countByChildIdAndProgressDateBetween(UUID childId, LocalDate startDate, LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MissionDailyProgress> findWithLockByChildIdAndMissionIdAndProgressDate(UUID childId, UUID missionId, LocalDate progressDate);
}
