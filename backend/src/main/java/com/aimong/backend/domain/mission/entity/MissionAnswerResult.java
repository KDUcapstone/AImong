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

@Getter
@Entity
@Table(name = "mission_answer_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionAnswerResult {

    @Id
    @Column(name = "result_id", nullable = false)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "is_review", nullable = false)
    private boolean review;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static MissionAnswerResult create(
            UUID attemptId,
            UUID childId,
            UUID missionId,
            UUID questionId,
            boolean isReview,
            boolean isCorrect
    ) {
        MissionAnswerResult result = new MissionAnswerResult();
        result.id = UUID.randomUUID();
        result.attemptId = attemptId;
        result.childId = childId;
        result.missionId = missionId;
        result.questionId = questionId;
        result.review = isReview;
        result.correct = isCorrect;
        return result;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
