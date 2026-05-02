package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuestionQualityIssue;
import com.aimong.backend.domain.mission.entity.QuestionQualityIssueSource;
import com.aimong.backend.domain.mission.entity.QuestionQualityIssueStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionQualityIssueRepository extends JpaRepository<QuestionQualityIssue, UUID> {

    long countByQuestionIdAndIssueSourceAndIssueStatusIn(
            UUID questionId,
            QuestionQualityIssueSource issueSource,
            Collection<QuestionQualityIssueStatus> issueStatuses
    );
}
