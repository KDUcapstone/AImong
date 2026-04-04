package com.aimong.backend.domain.streak.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "streak_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StreakRecord {

    // child_id가 PK
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

    public void incrementMissionCount() {
        this.todayMissionCount++;
    }

    public void completeDayStreak(LocalDate date) {
        this.continuousDays++;
        this.lastCompletedDate = date;
    }

    public void breakStreak() {
        this.continuousDays = 0;
    }
}
