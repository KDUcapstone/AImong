package com.aimong.backend.domain.streak.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 스트릭 마일스톤 지급 기록
 * milestone_days: 7일 or 30일 달성 시 티켓 지급
 * UNIQUE(child_id, milestone_days) → 중복 지급 방지
 */
@Entity
@Table(name = "milestone_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MilestoneReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "milestone_days", nullable = false)
    private Short milestoneDays;  // 7 or 30

    @Column(name = "rewarded_at", nullable = false, updatable = false)
    private OffsetDateTime rewardedAt;

    @PrePersist
    protected void onCreate() {
        if (rewardedAt == null) rewardedAt = OffsetDateTime.now();
    }
}
