package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "quiz_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttempt {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "set_id", length = 32)
    private String setId;

    @Column(name = "level_no")
    private Integer levelNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids_json", nullable = false, columnDefinition = "jsonb")
    private String questionIdsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_review", nullable = false)
    private boolean isReview;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    public static QuizAttempt create(UUID childId, UUID missionId, String questionIdsJson, Instant expiresAt, boolean isReview) {
        return create(childId, missionId, null, null, questionIdsJson, expiresAt, isReview);
    }

    public static QuizAttempt create(
            UUID childId,
            UUID missionId,
            String setId,
            Integer levelNo,
            String questionIdsJson,
            Instant expiresAt,
            boolean isReview
    ) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.id = UUID.randomUUID();
        attempt.childId = childId;
        attempt.missionId = missionId;
        attempt.setId = setId;
        attempt.levelNo = levelNo;
        attempt.questionIdsJson = questionIdsJson;
        attempt.expiresAt = expiresAt;
        attempt.isReview = isReview;
        return attempt;
    }

    public void markSubmitted(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
