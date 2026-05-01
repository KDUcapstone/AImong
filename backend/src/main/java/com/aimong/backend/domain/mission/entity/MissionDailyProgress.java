package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mission_daily_progress")
@IdClass(MissionDailyProgress.MissionDailyProgressId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionDailyProgress {

    @Id
    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Id
    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Id
    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;

    @Column(name = "first_attempt_at", nullable = false)
    private Instant firstAttemptAt;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "first_xp_earned", nullable = false)
    private int firstXpEarned;

    @Column(name = "review_attempt_count", nullable = false)
    private int reviewAttemptCount;

    public static MissionDailyProgress create(
            UUID childId,
            UUID missionId,
            LocalDate progressDate,
            Instant firstAttemptAt,
            int score,
            int total,
            int firstXpEarned
    ) {
        MissionDailyProgress progress = new MissionDailyProgress();
        progress.childId = childId;
        progress.missionId = missionId;
        progress.progressDate = progressDate;
        progress.firstAttemptAt = firstAttemptAt;
        progress.bestScore = score;
        progress.total = total;
        progress.firstXpEarned = firstXpEarned;
        progress.reviewAttemptCount = 0;
        return progress;
    }

    public void applyReviewAttempt(int score) {
        bestScore = Math.max(bestScore, score);
        reviewAttemptCount += 1;
    }

    @NoArgsConstructor
    public static class MissionDailyProgressId implements Serializable {

        private UUID childId;
        private UUID missionId;
        private LocalDate progressDate;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MissionDailyProgressId that)) {
                return false;
            }
            return Objects.equals(childId, that.childId)
                    && Objects.equals(missionId, that.missionId)
                    && Objects.equals(progressDate, that.progressDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(childId, missionId, progressDate);
        }
    }
}
