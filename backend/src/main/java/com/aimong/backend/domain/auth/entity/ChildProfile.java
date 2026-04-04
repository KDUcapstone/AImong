package com.aimong.backend.domain.auth.entity;

import com.aimong.backend.global.enums.ProfileImageType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChildProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentAccount parent;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    // child_code DOMAIN (6자리 숫자 TEXT)
    @Column(name = "code", nullable = false, unique = true)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_image_type", nullable = false,
            columnDefinition = "profile_image_type_enum")
    @Builder.Default
    private ProfileImageType profileImageType = ProfileImageType.DEFAULT;

    @Column(name = "session_version", nullable = false)
    @Builder.Default
    private Integer sessionVersion = 1;

    @Column(name = "today_xp_date")
    private LocalDate todayXpDate;

    @Column(name = "weekly_xp_week_start")
    private LocalDate weeklyXpWeekStart;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_active_at")
    private OffsetDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    // ── 비즈니스 메서드 ──

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
