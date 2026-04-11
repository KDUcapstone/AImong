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
@Table(name = "streak_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StreakRecord {

    @Id
    @Column(name = "child_id")
    private UUID childId;

    @Column(name = "continuous_days", nullable = false)
    private int continuousDays;

    @Column(name = "today_mission_count", nullable = false)
    private int todayMissionCount;

    @Column(name = "shield_count", nullable = false)
    private int shieldCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static StreakRecord create(UUID childId) {
        return new StreakRecord(childId, 0, 0, 0, null);
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
