package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionAttempt;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionAttemptRepository extends JpaRepository<MissionAttempt, UUID> {

    boolean existsByChildIdAndMissionIdAndAttemptDate(UUID childId, UUID missionId, LocalDate attemptDate);

    long countByChildIdAndMissionIdAndAttemptDate(UUID childId, UUID missionId, LocalDate attemptDate);

    long countByChildIdAndAttemptDateAndReviewFalseAndPassedTrue(UUID childId, LocalDate attemptDate);

    long countByChildIdAndAttemptDateBetweenAndReviewFalseAndPassedTrue(UUID childId, LocalDate startDate, LocalDate endDate);

    long countByChildIdAndReviewFalseAndPassedTrue(UUID childId);

    @Query("""
            select count(distinct ma.missionId)
            from MissionAttempt ma
            join Mission m on m.id = ma.missionId
            where ma.childId = :childId
              and ma.review = false
              and ma.passed = true
              and m.stage = :stage
            """)
    long countCompletedMissionByStage(
            @Param("childId") UUID childId,
            @Param("stage") short stage
    );

    @Query("""
            select max(ma.attemptDate)
            from MissionAttempt ma
            where ma.childId = :childId
              and ma.missionId = :missionId
              and ma.review = false
              and ma.passed = true
            """)
    Optional<LocalDate> findLatestCompletedAt(
            @Param("childId") UUID childId,
            @Param("missionId") UUID missionId
    );

}
