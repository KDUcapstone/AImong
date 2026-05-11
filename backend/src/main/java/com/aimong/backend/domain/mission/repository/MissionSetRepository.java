package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionSetRepository extends JpaRepository<MissionSet, String> {

    List<MissionSet> findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc();

    List<MissionSet> findAllByMissionIdAndActiveTrueOrderByStarLevelAscVariantNoAscSetIdAsc(UUID missionId);

    List<MissionSet> findAllByMissionIdAndStarLevelAndActiveTrueOrderByVariantNoAscSetIdAsc(UUID missionId, int starLevel);

    Optional<MissionSet> findBySetIdAndActiveTrue(String setId);

    long countByActiveTrue();
}
