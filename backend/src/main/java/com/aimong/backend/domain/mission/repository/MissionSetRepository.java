package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionSetRepository extends JpaRepository<MissionSet, String> {

    List<MissionSet> findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc();

    List<MissionSet> findAllByMissionIdAndActiveTrueOrderByStarLevelAscVariantNoAscSetIdAsc(UUID missionId);

    List<MissionSet> findAllByMissionIdAndStarLevelAndActiveTrueOrderByVariantNoAscSetIdAsc(UUID missionId, int starLevel);

    Optional<MissionSet> findBySetIdAndActiveTrue(String setId);

    long countByActiveTrue();

    @Query("""
            select count(distinct s.missionId)
            from MissionSet s
            where s.active = true
              and s.stage = :stage
              and s.starLevel = 1
            """)
    long countActiveStageStarOneMissionIds(@Param("stage") short stage);
}
