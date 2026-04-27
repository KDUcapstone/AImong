package com.aimong.backend.domain.streak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "last_completed_date")
    private LocalDate lastCompletedDate;

    @Column(name = "today_mission_count", nullable = false)
    private int todayMissionCount;

    @Column(name = "shield_count", nullable = false)
    private int shieldCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static StreakRecord create(UUID childId) {
        return new StreakRecord(childId, 0, null, 0, 0, null);
    }

    public void recordMissionCompletion(LocalDate today) {
        if (lastCompletedDate == null || lastCompletedDate.isBefore(today.minusDays(1))) {
            continuousDays = 1;
            todayMissionCount = 1;
        } else if (lastCompletedDate.equals(today.minusDays(1))) {
            continuousDays += 1;
            todayMissionCount = 1;
        } else if (lastCompletedDate.equals(today)) {
            todayMissionCount += 1;
        } else {
            continuousDays = 1;
            todayMissionCount = 1;
        }

        lastCompletedDate = today;
        updatedAt = Instant.now();
    }

    public void addShield(int count) {
        shieldCount += count;
        updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
