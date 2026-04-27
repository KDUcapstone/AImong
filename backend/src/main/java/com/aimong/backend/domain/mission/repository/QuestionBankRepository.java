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

    List<QuestionBank> findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(UUID missionId, QuestionPoolStatus questionPoolStatus);

    List<QuestionBank> findAllByMissionIdAndIsActiveTrueAndPackNoOrderByCreatedAtAsc(UUID missionId, Short packNo);

    List<QuestionBank> findAllByIdIn(Collection<UUID> ids);

    long countByMissionIdAndIsActiveTrue(UUID missionId);

    long countByMissionIdAndIsActiveTrueAndPackNo(UUID missionId, Short packNo);

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
