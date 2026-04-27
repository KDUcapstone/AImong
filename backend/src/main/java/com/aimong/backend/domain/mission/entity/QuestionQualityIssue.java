package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "question_quality_issues", schema = "private")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionQualityIssue {

    @Id
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "reported_by_child_id")
    private UUID reportedByChildId;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_source", nullable = false, length = 32)
    private QuestionQualityIssueSource issueSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false, length = 32)
    private QuestionQualityIssueStatus issueStatus;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "detail_text", columnDefinition = "text")
    private String detailText;

    @Column(name = "validation_decision", length = 32)
    private String validationDecision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hard_fail_reasons", columnDefinition = "jsonb")
    private String hardFailReasonsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repair_hints", columnDefinition = "jsonb")
    private String repairHintsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static QuestionQualityIssue createUserReport(
            UUID questionId,
            UUID missionId,
            UUID reportedByChildId,
            String reasonCode,
            String detailText
    ) {
        QuestionQualityIssue issue = new QuestionQualityIssue();
        issue.id = UUID.randomUUID();
        issue.questionId = questionId;
        issue.missionId = missionId;
        issue.reportedByChildId = reportedByChildId;
        issue.issueSource = QuestionQualityIssueSource.USER_REPORT;
        issue.issueStatus = QuestionQualityIssueStatus.OPEN;
        issue.reasonCode = reasonCode;
        issue.detailText = detailText;
        issue.createdAt = Instant.now();
        issue.updatedAt = issue.createdAt;
        return issue;
    }

    public static QuestionQualityIssue createServingRevalidationIssue(
            UUID questionId,
            UUID missionId,
            String reasonCode,
            String detailText,
            String validationDecision,
            String hardFailReasonsJson,
            String repairHintsJson
    ) {
        QuestionQualityIssue issue = new QuestionQualityIssue();
        issue.id = UUID.randomUUID();
        issue.questionId = questionId;
        issue.missionId = missionId;
        issue.issueSource = QuestionQualityIssueSource.SERVING_REVALIDATION;
        issue.issueStatus = QuestionQualityIssueStatus.QUARANTINED;
        issue.reasonCode = reasonCode;
        issue.detailText = detailText;
        issue.validationDecision = validationDecision;
        issue.hardFailReasonsJson = hardFailReasonsJson;
        issue.repairHintsJson = repairHintsJson;
        issue.createdAt = Instant.now();
        issue.updatedAt = issue.createdAt;
        return issue;
    }

    public void markQuarantined() {
        this.issueStatus = QuestionQualityIssueStatus.QUARANTINED;
        this.updatedAt = Instant.now();
    }
}
