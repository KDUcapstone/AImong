package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_reward_claims")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReturnRewardClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    // 수령 시점의 streak_records.last_completed_date — 중복 수령 방지 기준
    @Column(name = "base_last_completed_date", nullable = false)
    private LocalDate baseLastCompletedDate;

    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;  // 1~3

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private OffsetDateTime claimedAt;

    @PrePersist
    protected void onCreate() {
        if (claimedAt == null) claimedAt = OffsetDateTime.now();
    }
}
