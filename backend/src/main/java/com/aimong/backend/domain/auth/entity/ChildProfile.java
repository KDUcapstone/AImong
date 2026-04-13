package com.aimong.backend.domain.auth.entity;

import com.aimong.backend.domain.pet.entity.PetGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "child_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChildProfile {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentAccount parentAccount;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "code", nullable = false, unique = true, length = 6)
    private String code;

    @Column(name = "starter_issued", nullable = false)
    private boolean starterIssued;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(name = "today_xp", nullable = false)
    private int todayXp;

    @Column(name = "weekly_xp", nullable = false)
    private int weeklyXp;

    @Column(name = "today_xp_date")
    private LocalDate todayXpDate;

    @Column(name = "weekly_xp_week_start")
    private LocalDate weeklyXpWeekStart;

    @Column(name = "gacha_pull_count", nullable = false)
    private int gachaPullCount;

    @Column(name = "sr_miss_count", nullable = false)
    private int srMissCount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "profile_image_type", nullable = false)
    private ProfileImageType profileImageType;

    @Column(name = "session_version", nullable = false)
    private int sessionVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    public static ChildProfile create(ParentAccount parentAccount, String nickname, String code) {
        return new ChildProfile(
                UUID.randomUUID(),
                parentAccount,
                nickname,
                code,
                true,
                0,
                0,
                0,
                null,
                null,
                0,
                0,
                ProfileImageType.DEFAULT,
                1,
                null,
                null
        );
    }

    public void regenerateCode(String newCode) {
        this.code = newCode;
        this.sessionVersion += 1;
    }

    public void applyMissionXp(int xpEarned, LocalDate today, LocalDate weekStart) {
        if (todayXpDate == null || !todayXpDate.equals(today)) {
            todayXp = 0;
            todayXpDate = today;
        }
        if (weeklyXpWeekStart == null || !weeklyXpWeekStart.equals(weekStart)) {
            weeklyXp = 0;
            weeklyXpWeekStart = weekStart;
        }

        totalXp += xpEarned;
        todayXp += xpEarned;
        weeklyXp += xpEarned;
    }

    public int getLevel() {
        return (totalXp / 100) + 1;
    }

    public int getNextLevelTargetXp() {
        return getLevel() * 100;
    }

    public void refreshProfileImageType() {
        if (totalXp >= 1000) {
            profileImageType = ProfileImageType.GUARDIAN;
        } else if (totalXp >= 500) {
            profileImageType = ProfileImageType.CRITIC;
        } else if (totalXp >= 300) {
            profileImageType = ProfileImageType.EXPLORER;
        } else if (totalXp >= 100) {
            profileImageType = ProfileImageType.SPROUT;
        } else {
            profileImageType = ProfileImageType.DEFAULT;
        }
    }

    public void touchLastActiveAt(Instant instant) {
        this.lastActiveAt = instant;
    }

    public void recordGachaPull(PetGrade grade) {
        gachaPullCount += 1;
        if (grade == PetGrade.NORMAL) {
            srMissCount += 1;
            return;
        }
        srMissCount = 0;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
