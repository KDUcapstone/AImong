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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids_json", nullable = false, columnDefinition = "jsonb")
    private String questionIdsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    public static QuizAttempt create(UUID childId, UUID missionId, String questionIdsJson, Instant expiresAt) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.id = UUID.randomUUID();
        attempt.childId = childId;
        attempt.missionId = missionId;
        attempt.questionIdsJson = questionIdsJson;
        attempt.expiresAt = expiresAt;
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
