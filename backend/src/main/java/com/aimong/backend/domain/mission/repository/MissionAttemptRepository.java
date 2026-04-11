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

    Optional<MissionAttempt> findTopByChildIdAndMissionIdOrderBySubmittedAtDesc(UUID childId, UUID missionId);

    @Query("""
            select count(distinct ma.missionId)
            from MissionAttempt ma
            join Mission m on m.id = ma.missionId
            where ma.childId = :childId
              and ma.attemptNo = 1
              and ma.score * :passScoreDenominator >= ma.total * :passScoreNumerator
              and m.stage = :stage
            """)
    long countCompletedMissionByStage(
            @Param("childId") UUID childId,
            @Param("stage") short stage,
            @Param("passScoreNumerator") int passScoreNumerator,
            @Param("passScoreDenominator") int passScoreDenominator
    );

    @Query("""
            select max(ma.attemptDate)
            from MissionAttempt ma
            where ma.childId = :childId
              and ma.missionId = :missionId
              and ma.score * :passScoreDenominator >= ma.total * :passScoreNumerator
            """)
    Optional<LocalDate> findLatestCompletedAt(
            @Param("childId") UUID childId,
            @Param("missionId") UUID missionId,
            @Param("passScoreNumerator") int passScoreNumerator,
            @Param("passScoreDenominator") int passScoreDenominator
    );
}
