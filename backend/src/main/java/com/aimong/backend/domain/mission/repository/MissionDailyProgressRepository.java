package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress.MissionDailyProgressId;
import com.aimong.backend.domain.parent.dto.ParentDailyProgressStat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionDailyProgressRepository extends JpaRepository<MissionDailyProgress, MissionDailyProgressId> {

    Optional<MissionDailyProgress> findByChildIdAndMissionIdAndProgressDate(UUID childId, UUID missionId, LocalDate progressDate);

    List<MissionDailyProgress> findAllByChildIdAndProgressDate(UUID childId, LocalDate progressDate);

    List<MissionDailyProgress> findAllByChildIdAndProgressDateBetweenOrderByProgressDateAsc(
            UUID childId,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByChildIdAndProgressDateBetween(UUID childId, LocalDate startDate, LocalDate endDate);

    @Query("""
            select new com.aimong.backend.domain.parent.dto.ParentDailyProgressStat(
                p.progressDate,
                count(p),
                coalesce(sum(p.firstXpEarned), 0)
            )
            from MissionDailyProgress p
            where p.childId = :childId
              and p.progressDate between :startDate and :endDate
            group by p.progressDate
            order by p.progressDate asc
            """)
    List<ParentDailyProgressStat> findDailyProgressStats(
            @Param("childId") UUID childId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MissionDailyProgress> findWithLockByChildIdAndMissionIdAndProgressDate(UUID childId, UUID missionId, LocalDate progressDate);
}
