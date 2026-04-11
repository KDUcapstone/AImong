package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {

    List<QuestionBank> findAllByMissionIdAndIsActiveTrue(UUID missionId);

    List<QuestionBank> findAllByIdIn(Collection<UUID> ids);
}
