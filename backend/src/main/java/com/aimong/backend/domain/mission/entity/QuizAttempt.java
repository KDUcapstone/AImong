package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "set_id", nullable = false, length = 32)
    private String setId;

    @Column(name = "star_level", nullable = false)
    private Integer starLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids_json", nullable = false, columnDefinition = "jsonb")
    private String questionIdsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answered_question_ids_json", nullable = false, columnDefinition = "jsonb")
    private String answeredQuestionIdsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_review", nullable = false)
    private boolean isReview;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "attempt_status_enum")
    private QuizAttemptStatus status;

    @Column(name = "abandoned_at")
    private Instant abandonedAt;

    @Column(name = "abandon_reason", length = 64)
    private String abandonReason;

    @Column(name = "remaining_lives", nullable = false)
    private int remainingLives;

    @Column(name = "wrong_count_in_session", nullable = false)
    private int wrongCountInSession;

    @Column(name = "revive_count", nullable = false)
    private int reviveCount;

    @Column(name = "revived_at")
    private Instant revivedAt;

    public static QuizAttempt create(UUID childId, UUID missionId, String questionIdsJson, Instant expiresAt, boolean isReview) {
        return create(childId, missionId, null, 1, questionIdsJson, expiresAt, isReview);
    }

    public static QuizAttempt create(
            UUID childId,
            UUID missionId,
            String setId,
            Integer starLevel,
            String questionIdsJson,
            Instant expiresAt,
            boolean isReview
    ) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.id = UUID.randomUUID();
        attempt.childId = childId;
        attempt.missionId = missionId;
        attempt.setId = setId;
        attempt.starLevel = starLevel;
        attempt.questionIdsJson = questionIdsJson;
        attempt.answeredQuestionIdsJson = "[]";
        attempt.expiresAt = expiresAt;
        attempt.isReview = isReview;
        attempt.status = QuizAttemptStatus.IN_PROGRESS;
        attempt.remainingLives = 3;
        attempt.wrongCountInSession = 0;
        attempt.reviveCount = 0;
        return attempt;
    }

    public void updateAnsweredQuestionIdsJson(String answeredQuestionIdsJson) {
        this.answeredQuestionIdsJson = answeredQuestionIdsJson;
    }

    public void markSubmitted(Instant submittedAt) {
        this.submittedAt = submittedAt;
        this.status = QuizAttemptStatus.SUBMITTED;
    }

    public void markExpired() {
        this.status = QuizAttemptStatus.EXPIRED;
    }

    public void abandon(String reason, Instant abandonedAt) {
        this.status = QuizAttemptStatus.ABANDONED;
        this.abandonedAt = abandonedAt;
        this.abandonReason = reason;
    }

    public void recordWrongAnswer() {
        wrongCountInSession += 1;
        if (remainingLives > 0) {
            remainingLives -= 1;
        }
    }

    public boolean canRevive() {
        return !isReview && status == QuizAttemptStatus.IN_PROGRESS && remainingLives == 0 && reviveCount == 0;
    }

    public void revive(Instant revivedAt) {
        remainingLives = 3;
        reviveCount += 1;
        this.revivedAt = revivedAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = QuizAttemptStatus.IN_PROGRESS;
        }
        if (answeredQuestionIdsJson == null) {
            answeredQuestionIdsJson = "[]";
        }
        if (remainingLives == 0 && wrongCountInSession == 0 && reviveCount == 0) {
            remainingLives = 3;
        }
    }
}
