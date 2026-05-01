package com.aimong.backend.domain.streak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "milestone_rewards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MilestoneReward {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "milestone_days", nullable = false)
    private short milestoneDays;

    @Column(name = "rewarded_at", nullable = false)
    private Instant rewardedAt;

    public static MilestoneReward create(UUID childId, short milestoneDays) {
        return new MilestoneReward(UUID.randomUUID(), childId, milestoneDays, null);
    }

    @PrePersist
    void prePersist() {
        if (rewardedAt == null) {
            rewardedAt = Instant.now();
        }
    }
}
