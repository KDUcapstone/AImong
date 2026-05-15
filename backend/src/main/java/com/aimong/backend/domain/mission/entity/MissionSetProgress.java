package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mission_set_progress")
@IdClass(MissionSetProgress.MissionSetProgressId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSetProgress {

    @Id
    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Id
    @Column(name = "set_id", nullable = false, length = 32)
    private String setId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "stage", nullable = false)
    private Integer stage;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "best_score")
    private Integer bestScore;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "first_passed_attempt_id")
    private UUID firstPassedAttemptId;

    @Column(name = "star_level", nullable = false)
    private Integer starLevel;

    @Column(name = "variant_no", nullable = false)
    private Integer variantNo;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static MissionSetProgress create(
            UUID childId,
            String setId,
            UUID missionId,
            Integer stage,
            Integer starLevel,
            Integer variantNo,
            UUID firstPassedAttemptId,
            int score,
            int total
    ) {
        MissionSetProgress progress = new MissionSetProgress();
        progress.childId = childId;
        progress.setId = setId;
        progress.missionId = missionId;
        progress.stage = stage;
        progress.starLevel = starLevel;
        progress.variantNo = variantNo;
        progress.firstPassedAttemptId = firstPassedAttemptId;
        progress.completedAt = Instant.now();
        progress.bestScore = score;
        progress.total = total;
        progress.completed = true;
        progress.updatedAt = Instant.now();
        return progress;
    }

    public void improveBestScore(int score) {
        bestScore = bestScore == null ? score : Math.max(bestScore, score);
        updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @NoArgsConstructor
    public static class MissionSetProgressId implements Serializable {
        private UUID childId;
        private String setId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MissionSetProgressId that)) {
                return false;
            }
            return Objects.equals(childId, that.childId) && Objects.equals(setId, that.setId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(childId, setId);
        }
    }
}
