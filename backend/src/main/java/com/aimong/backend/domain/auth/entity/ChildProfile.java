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
import java.time.Duration;
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

    public static final int MAX_ENERGY = 20;
    public static final int MISSION_ENERGY_COST = 5;
    public static final int ENERGY_RECOVERY_MINUTES = 10;

    @Id
    @Column(name = "child_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentAccount parentAccount;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "code", unique = true, length = 6)
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

    @Column(name = "shield_count", nullable = false)
    private int shieldCount;

    @Column(name = "gear", nullable = false)
    private int gear;

    @Column(name = "equipped_pet_id")
    private UUID equippedPetId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "profile_image_type", nullable = false)
    private ProfileImageType profileImageType;

    @Column(name = "session_version", nullable = false)
    private int sessionVersion;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "energy", nullable = false)
    private int energy;

    @Column(name = "energy_recovered_at", nullable = false)
    private Instant energyRecoveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static ChildProfile create(ParentAccount parentAccount, String nickname, String code) {
        return new ChildProfile(
                UUID.randomUUID(),
                parentAccount,
                nickname,
                code,
                false,
                0,
                0,
                0,
                null,
                null,
                0,
                0,
                0,
                0,
                null,
                ProfileImageType.DEFAULT,
                0,
                null,
                MAX_ENERGY,
                null,
                null,
                null,
                null
        );
    }

    public void regenerateCode(String newCode) {
        this.code = newCode;
        this.sessionVersion += 1;
    }

    public void markStarterIssued() {
        this.starterIssued = true;
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

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void clearFcmToken() {
        this.fcmToken = null;
    }

    public void updateProfile(String nickname, ProfileImageType profileImageType) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageType != null) {
            this.profileImageType = profileImageType;
        }
    }

    public void logout() {
        this.sessionVersion += 1;
        this.fcmToken = null;
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.code = null;
        this.fcmToken = null;
        this.sessionVersion += 1;
    }

    public void addShield(int count) {
        shieldCount += count;
    }

    public void addGear(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        gear += amount;
    }

    public boolean consumeGear(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (gear < amount) {
            return false;
        }
        gear -= amount;
        return true;
    }

    public void recoverEnergy(Instant now) {
        if (energyRecoveredAt == null) {
            energyRecoveredAt = now;
        }
        if (energy >= MAX_ENERGY) {
            energy = MAX_ENERGY;
            energyRecoveredAt = now;
            return;
        }

        long recoveredUnits = Duration.between(energyRecoveredAt, now).toMinutes() / ENERGY_RECOVERY_MINUTES;
        if (recoveredUnits <= 0) {
            return;
        }
        int recoveredEnergy = (int) Math.min(recoveredUnits, MAX_ENERGY - energy);
        energy += recoveredEnergy;
        energyRecoveredAt = energy >= MAX_ENERGY
                ? now
                : energyRecoveredAt.plus(Duration.ofMinutes(recoveredUnits * ENERGY_RECOVERY_MINUTES));
    }

    public boolean consumeMissionEnergy(Instant now) {
        recoverEnergy(now);
        if (energy < MISSION_ENERGY_COST) {
            return false;
        }
        energy -= MISSION_ENERGY_COST;
        return true;
    }

    public void addEnergy(int amount, Instant now) {
        recoverEnergy(now);
        energy = Math.min(MAX_ENERGY, energy + amount);
        if (energy >= MAX_ENERGY) {
            energyRecoveredAt = now;
        }
    }

    public Instant nextEnergyRecoverAt() {
        if (energy >= MAX_ENERGY) {
            return null;
        }
        Instant base = energyRecoveredAt == null ? Instant.now() : energyRecoveredAt;
        return base.plus(Duration.ofMinutes(ENERGY_RECOVERY_MINUTES));
    }

    public boolean consumeShieldIfAvailable() {
        if (shieldCount <= 0) {
            return false;
        }
        shieldCount -= 1;
        return true;
    }

    public void equipPet(UUID petId) {
        this.equippedPetId = petId;
    }

    public void recordGachaPull(PetGrade grade) {
        gachaPullCount += 1;
        recordGachaResult(grade);
    }

    public void recordGachaResult(PetGrade grade) {
        if (grade == PetGrade.EPIC || grade == PetGrade.LEGEND) {
            srMissCount = 0;
        } else {
            srMissCount += 1;
        }
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (energyRecoveredAt == null) {
            energyRecoveredAt = createdAt;
        }
    }
}
