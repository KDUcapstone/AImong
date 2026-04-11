package com.aimong.backend.domain.mission.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.OffsetDateTime;

/**
 * 당일 미션 진행 요약.
 * 최초 시도 시각, 최고 점수, 첫 적립 XP, 복습 횟수를 API 조회용으로 유지한다.
 */
@Entity
@Table(name = "mission_daily_progress")
@Check(constraints = "best_score <= total")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MissionDailyProgress {

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
        this.bestScore = Math.max(this.bestScore, score);
    }

    public void incrementReviewAttemptCount() {
        this.reviewAttemptCount++;
    }
}
