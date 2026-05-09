package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionSetRepository extends JpaRepository<MissionSet, String> {

    List<MissionSet> findAllByActiveTrueOrderByLevelNoAscStageAscDisplayOrderAscSetIdAsc();

    List<MissionSet> findAllByMissionIdAndActiveTrueOrderByLevelNoAscDisplayOrderAscSetIdAsc(UUID missionId);

    Optional<MissionSet> findBySetIdAndActiveTrue(String setId);

    long countByActiveTrue();
}
