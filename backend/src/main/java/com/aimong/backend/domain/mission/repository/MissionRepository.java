package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.Mission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

    List<Mission> findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc();

    long countByIsActiveTrue();
}
