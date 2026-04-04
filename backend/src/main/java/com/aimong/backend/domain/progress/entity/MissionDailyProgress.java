package com.aimong.backend.domain.progress.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.mission.entity.Mission;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "mission_daily_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MissionDailyProgress {

    // PK: (child_id, mission_id, progress_date)
    @EmbeddedId
    private MissionDailyProgressId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("childId")
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("missionId")
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "first_attempt_at", nullable = false)
    private OffsetDateTime firstAttemptAt;

    @Column(name = "best_score", nullable = false)
    private Integer bestScore;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "first_xp_earned", nullable = false)
    private Integer firstXpEarned;

    @Column(name = "review_attempt_count", nullable = false)
    @Builder.Default
    private Integer reviewAttemptCount = 0;

    public void updateBestScore(int score) {
        if (score > this.bestScore) this.bestScore = score;
    }

    public void incrementReviewCount() {
        this.reviewAttemptCount++;
    }
}
