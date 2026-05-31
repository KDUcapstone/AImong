package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuestionQualityIssue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionQualityIssueRepository extends JpaRepository<QuestionQualityIssue, UUID> {
}
