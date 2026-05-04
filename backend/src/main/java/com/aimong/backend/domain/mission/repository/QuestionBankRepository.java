package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {

    List<QuestionBank> findAllByMissionIdAndIsActiveTrue(UUID missionId);

    List<QuestionBank> findAllByMissionIdAndIsActiveTrueAndDifficulty(UUID missionId, DifficultyBand difficulty);

    @Query(value = """
            SELECT
                id,
                mission_id,
                question_type,
                prompt,
                options,
                content_tags,
                curriculum_ref,
                difficulty,
                legacy_numeric_difficulty,
                source_type,
                generation_phase,
                pack_no,
                difficulty_band,
                question_pool_status,
                created_at,
                is_active
            FROM public.question_bank
            WHERE mission_id = :missionId
              AND difficulty = CAST(:difficulty AS difficulty_band_enum)
              AND is_active = TRUE
              AND question_pool_status = CAST('ACTIVE' AS question_pool_status_enum)
            """, nativeQuery = true)
    List<QuestionBank> findAllFromSafeViewByMissionIdAndDifficulty(
            @Param("missionId") UUID missionId,
            @Param("difficulty") String difficulty
    );

    List<QuestionBank> findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(UUID missionId, QuestionPoolStatus questionPoolStatus);

    List<QuestionBank> findAllByMissionIdAndIsActiveTrueAndPackNoOrderByCreatedAtAsc(UUID missionId, Short packNo);

    java.util.Optional<QuestionBank> findByIdAndMissionIdAndIsActiveTrue(UUID id, UUID missionId);

    List<QuestionBank> findAllByIdIn(Collection<UUID> ids);

    long countByIsActiveTrue();

    long countByMissionIdAndIsActiveTrue(UUID missionId);

    long countByMissionIdAndIsActiveTrueAndPackNo(UUID missionId, Short packNo);

    long countByMissionIdAndIsActiveTrueAndDifficulty(UUID missionId, DifficultyBand difficulty);

    long countByMissionIdAndIsActiveTrueAndDifficultyBand(UUID missionId, DifficultyBand difficultyBand);

    @Query("""
            select q.packNo
            from QuestionBank q
            where q.missionId = :missionId
              and q.isActive = true
              and q.questionPoolStatus = com.aimong.backend.domain.mission.entity.QuestionPoolStatus.ACTIVE
            group by q.packNo
            having count(q.id) = :expectedPackSize
            order by q.packNo asc
            """)
    List<Short> findIntactPackNumbers(
            @Param("missionId") UUID missionId,
            @Param("expectedPackSize") long expectedPackSize
    );
}
