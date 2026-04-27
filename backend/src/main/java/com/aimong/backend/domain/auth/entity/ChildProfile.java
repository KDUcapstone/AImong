package com.aimong.backend.domain.auth.entity;

import com.aimong.backend.global.enums.ProfileImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "child_profiles",
    indexes = {
        @Index(name = "idx_child_profiles_parent", columnList = "parent_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChildProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "child_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID childId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentAccount parent;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "code", nullable = false, unique = true, length = 6)
    private String code;

    @Column(name = "starter_issued", nullable = false)
    @Builder.Default
    private Boolean starterIssued = false;

    @Column(name = "total_xp", nullable = false)
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "today_xp", nullable = false)
    @Builder.Default
    private Integer todayXp = 0;

    @Column(name = "weekly_xp", nullable = false)
    @Builder.Default
    private Integer weeklyXp = 0;

    @Column(name = "gacha_pull_count", nullable = false)
    @Builder.Default
    private Integer gachaPullCount = 0;

    @Column(name = "sr_miss_count", nullable = false)
    @Builder.Default
    private Integer srMissCount = 0;

    @Column(name = "shield_count", nullable = false)
    @Builder.Default
    private Integer shieldCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_image_type", nullable = false, columnDefinition = "profile_image_type_enum")
    @Builder.Default
    private ProfileImageType profileImageType = ProfileImageType.DEFAULT;

    @Column(name = "equipped_pet_id", columnDefinition = "uuid")
    private UUID equippedPetId;

    @Column(name = "session_version", nullable = false)
    @Builder.Default
    private Integer sessionVersion = 1;

    @Column(name = "today_xp_date")
    private LocalDate todayXpDate;

    @Column(name = "weekly_xp_week_start")
    private LocalDate weeklyXpWeekStart;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_active_at")
    private OffsetDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public void addXp(int xp) {
        this.totalXp += xp;
        this.todayXp += xp;
        this.weeklyXp += xp;
    }

    public void incrementSessionVersion() {
        this.sessionVersion++;
    }

    public void updateLastActive() {
        this.lastActiveAt = OffsetDateTime.now();
    }
}
