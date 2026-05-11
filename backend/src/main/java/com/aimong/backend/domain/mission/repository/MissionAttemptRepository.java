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

    @Query(value = """
            select count(distinct coalesce(ma.set_id, ma.mission_id::text))
            from mission_attempts ma
            where ma.child_id = :childId
              and ma.is_review = false
              and ma.is_passed = true
            """, nativeQuery = true)
    long countCompletedMission(@Param("childId") UUID childId);

    @Query(value = """
            with first_completed as (
                select coalesce(ma.set_id, ma.mission_id::text) as completion_key,
                       min(ma.attempt_date) as first_completed_date
                from mission_attempts ma
                where ma.child_id = :childId
                  and ma.is_review = false
                  and ma.is_passed = true
                group by coalesce(ma.set_id, ma.mission_id::text)
            )
            select count(*)
            from first_completed fc
            where fc.first_completed_date between :startDate and :endDate
            """, nativeQuery = true)
    long countFirstCompletedMissionBetween(
            @Param("childId") UUID childId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
            select count(distinct coalesce(ma.set_id, ma.mission_id::text))
            from mission_attempts ma
            join missions m on m.id = ma.mission_id
            left join mission_sets ms on ms.set_id = ma.set_id
            where ma.child_id = :childId
              and ma.is_review = false
              and ma.is_passed = true
              and coalesce(ms.stage, m.stage) = :stage
            """, nativeQuery = true)
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
