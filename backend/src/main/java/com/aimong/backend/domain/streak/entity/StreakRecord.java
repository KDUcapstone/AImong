package com.aimong.backend.domain.streak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private StreakStatus status;

    @Column(name = "recovery_deadline_date")
    private LocalDate recoveryDeadlineDate;

    @Column(name = "recovery_base_days")
    private Integer recoveryBaseDays;

    @Column(name = "last_shield_used_date")
    private LocalDate lastShieldUsedDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static StreakRecord create(UUID childId) {
        return new StreakRecord(childId, 0, null, 0, 0, StreakStatus.ACTIVE, null, null, null, null);
    }

    public void recordMissionCompletion(LocalDate today) {
        if (status == null) {
            status = StreakStatus.ACTIVE;
        }

        if (status == StreakStatus.RECOVERABLE && recoveryDeadlineDate != null && !today.isAfter(recoveryDeadlineDate)) {
            continuousDays = (recoveryBaseDays == null ? continuousDays : recoveryBaseDays) + 1;
            todayMissionCount = 1;
            lastCompletedDate = today;
            clearRecovery(StreakStatus.ACTIVE);
            updatedAt = Instant.now();
            return;
        }

        if (status == StreakStatus.RECOVERABLE && recoveryDeadlineDate != null && today.isAfter(recoveryDeadlineDate)) {
            breakStreak();
        }

        if (status == StreakStatus.PROTECTED
                && lastShieldUsedDate != null
                && lastShieldUsedDate.equals(today.minusDays(1))) {
            continuousDays += 1;
            todayMissionCount = 1;
            lastCompletedDate = today;
            clearRecovery(StreakStatus.ACTIVE);
            updatedAt = Instant.now();
            return;
        }

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
        clearRecovery(StreakStatus.ACTIVE);
        updatedAt = Instant.now();
    }

    public void resetStreak() {
        breakStreak();
    }

    public void breakStreak() {
        continuousDays = 0;
        todayMissionCount = 0;
        clearRecovery(StreakStatus.BROKEN);
        updatedAt = Instant.now();
    }

    public void resetTodayMissionCount() {
        todayMissionCount = 0;
        updatedAt = Instant.now();
    }

    public void markProtectedByShield(LocalDate protectedDate) {
        todayMissionCount = 0;
        lastShieldUsedDate = protectedDate;
        clearRecovery(StreakStatus.PROTECTED);
        updatedAt = Instant.now();
    }

    public void markRecoverable(LocalDate recoveryDeadlineDate) {
        todayMissionCount = 0;
        status = StreakStatus.RECOVERABLE;
        this.recoveryDeadlineDate = recoveryDeadlineDate;
        this.recoveryBaseDays = continuousDays;
        updatedAt = Instant.now();
    }

    public boolean isRecoveryAvailable(LocalDate today) {
        return status == StreakStatus.RECOVERABLE
                && recoveryDeadlineDate != null
                && !today.isAfter(recoveryDeadlineDate);
    }

    public boolean expireRecoveryIfPast(LocalDate today) {
        if (status == StreakStatus.RECOVERABLE
                && recoveryDeadlineDate != null
                && today.isAfter(recoveryDeadlineDate)) {
            breakStreak();
            return true;
        }
        return false;
    }

    private void clearRecovery(StreakStatus nextStatus) {
        status = nextStatus;
        recoveryDeadlineDate = null;
        recoveryBaseDays = null;
    }

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = StreakStatus.ACTIVE;
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
