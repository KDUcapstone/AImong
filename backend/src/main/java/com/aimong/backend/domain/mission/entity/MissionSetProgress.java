package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "first_attempt_id")
    private UUID firstAttemptId;

    public static MissionSetProgress create(UUID childId, String setId, UUID firstAttemptId, int score, int total) {
        MissionSetProgress progress = new MissionSetProgress();
        progress.childId = childId;
        progress.setId = setId;
        progress.firstAttemptId = firstAttemptId;
        progress.completedAt = Instant.now();
        progress.bestScore = score;
        progress.total = total;
        return progress;
    }

    public void improveBestScore(int score) {
        bestScore = Math.max(bestScore, score);
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
