package com.aimong.backend.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "return_reward_claims")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReturnRewardClaim {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "base_last_completed_date", nullable = false)
    private LocalDate baseLastCompletedDate;

    @Column(name = "ticket_count", nullable = false)
    private int ticketCount;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    public static ReturnRewardClaim create(UUID childId, LocalDate baseLastCompletedDate, int ticketCount) {
        ReturnRewardClaim claim = new ReturnRewardClaim();
        claim.id = UUID.randomUUID();
        claim.childId = childId;
        claim.baseLastCompletedDate = baseLastCompletedDate;
        claim.ticketCount = ticketCount;
        claim.claimedAt = Instant.now();
        return claim;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (claimedAt == null) {
            claimedAt = Instant.now();
        }
    }
}
