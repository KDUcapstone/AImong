package com.aimong.backend.domain.quest.entity;

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
@Table(name = "weekly_quest_progress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WeeklyQuest {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "quest_type", nullable = false)
    private WeeklyQuestType questType;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "reward_claimed", nullable = false)
    private boolean rewardClaimed;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static WeeklyQuest create(UUID childId, LocalDate weekStart, WeeklyQuestType questType) {
        return new WeeklyQuest(UUID.randomUUID(), childId, weekStart, questType, false, false, null);
    }

    public void complete(boolean autoClaim) {
        if (completed) {
            if (autoClaim) {
                rewardClaimed = true;
            }
            return;
        }
        completed = true;
        rewardClaimed = autoClaim;
        completedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (completed && completedAt == null) {
            completedAt = Instant.now();
        }
    }
}
