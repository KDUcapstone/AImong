package com.aimong.backend.domain.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "achievements")
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

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    public static Achievement unlock(UUID childId, AchievementType achievementType) {
        return new Achievement(UUID.randomUUID(), childId, achievementType, null);
    }

    @PrePersist
    void prePersist() {
        if (unlockedAt == null) {
            unlockedAt = Instant.now();
        }
    }
}
