package com.aimong.backend.domain.streak.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 스트릭 기록 — 자녀당 1행(child_id PK)
 * 스케줄러가 매일 00:00 KST에 today_mission_count 0으로 초기화
 */
@Entity
@Table(name = "streak_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StreakRecord {

    @Id
    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "child_id")
    private ChildProfile child;

    @Column(name = "continuous_days", nullable = false)
    @Builder.Default
    private Integer continuousDays = 0;

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    @Column(name = "today_mission_count", nullable = false)
    @Builder.Default
    private Integer todayMissionCount = 0;

    /** 스트릭 방패 보유 수 — 하루 미완료 시 차감하여 연속일 유지 */
    @Column(name = "shield_count", nullable = false)
    @Builder.Default
    private Integer shieldCount = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void completeDayStreak(LocalDate date) {
        this.continuousDays++;
        this.lastCompletedDate = date;
    }

    public void breakStreak() {
        this.continuousDays = 0;
    }
}
