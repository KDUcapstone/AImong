package com.aimong.backend.domain.streak.entity;

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
@Table(name = "streak_milestones")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreakMilestone {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "target_days", nullable = false)
    private short targetDays;

    @Column(name = "tier", nullable = false)
    private short tier;

    @Column(name = "achieved", nullable = false)
    private boolean achieved;

    @Column(name = "reward_claimed", nullable = false)
    private boolean rewardClaimed;

    @Column(name = "achieved_at")
    private Instant achievedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static StreakMilestone create(UUID childId, short targetDays, short tier) {
        StreakMilestone milestone = new StreakMilestone();
        milestone.id = UUID.randomUUID();
        milestone.childId = childId;
        milestone.targetDays = targetDays;
        milestone.tier = tier;
        milestone.achieved = false;
        milestone.rewardClaimed = false;
        milestone.createdAt = Instant.now();
        return milestone;
    }

    public void achieveAndClaim() {
        achieved = true;
        rewardClaimed = true;
        achievedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
