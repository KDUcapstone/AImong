package com.aimong.backend.domain.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "achievement_progress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Achievement {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "achievement_type", nullable = false)
    private AchievementType achievementType;

    @Column(name = "current_value", nullable = false)
    private int currentValue;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private java.time.LocalDate completedAt;

    public static Achievement create(UUID childId, AchievementType achievementType) {
        return new Achievement(UUID.randomUUID(), childId, achievementType, 0, false, null);
    }

    public void updateProgress(int currentValue) {
        this.currentValue = Math.max(this.currentValue, currentValue);
    }

    public void complete(java.time.LocalDate completedAt) {
        this.completed = true;
        this.completedAt = completedAt;
    }

    public boolean isIncomplete() {
        return !completed;
    }

    @PrePersist
    void prePersist() {
        if (completed && completedAt == null) {
            completedAt = java.time.LocalDate.now();
        }
    }
}
