package com.aimong.backend.domain.quest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.AchievementType;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업적(히든 프로필) 달성 기록
 * total_xp 기준: SPROUT(100+) / EXPLORER(300+) / CRITIC(500+) / GUARDIAN(1000+)
 * 달성 시 profile_image_type도 함께 업데이트됨
 */
@Entity
@Table(name = "achievements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", nullable = false, columnDefinition = "achievement_type_enum")
    private AchievementType achievementType;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private OffsetDateTime unlockedAt;

    @PrePersist
    protected void onCreate() {
        if (unlockedAt == null) unlockedAt = OffsetDateTime.now();
    }
}
