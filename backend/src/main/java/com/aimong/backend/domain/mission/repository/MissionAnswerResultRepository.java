package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionAnswerResultRepository extends JpaRepository<MissionAnswerResult, UUID> {
}
