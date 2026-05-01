package com.aimong.backend.domain.streak.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "streak_milestones",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_streak_milestones_child_target", columnNames = {"child_id", "target_days"})
    },
    indexes = {
        @Index(name = "idx_streak_milestones_child", columnList = "child_id")
    }
)
@Check(constraints = "(reward_claimed = false OR achieved = true) AND ((achieved = false AND achieved_at IS NULL) OR (achieved = true AND achieved_at IS NOT NULL))")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StreakMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "target_days", nullable = false)
    private Short targetDays;

    @Column(name = "tier", nullable = false)
    private Short tier;

    @Column(name = "achieved", nullable = false)
    @Builder.Default
    private Boolean achieved = false;

    @Column(name = "reward_claimed", nullable = false)
    @Builder.Default
    private Boolean rewardClaimed = false;

    @Column(name = "achieved_at")
    private OffsetDateTime achievedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public void achieve() {
        this.achieved = true;
        this.achievedAt = OffsetDateTime.now();
    }

    public void claimReward() {
        this.rewardClaimed = true;
    }
}
