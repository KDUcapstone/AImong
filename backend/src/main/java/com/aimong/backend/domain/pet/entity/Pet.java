package com.aimong.backend.domain.pet.entity;

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
@Table(name = "pets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Pet {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "pet_type", nullable = false)
    private String petType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "grade", nullable = false)
    private PetGrade grade;

    @Column(name = "xp", nullable = false)
    private int xp;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "stage", nullable = false)
    private PetStage stage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mood", nullable = false)
    private PetMood mood;

    @Column(name = "crown_unlocked", nullable = false)
    private boolean crownUnlocked;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "crown_type")
    private CrownType crownType;

    @Column(name = "obtained_at", nullable = false)
    private Instant obtainedAt;

    public static Pet create(UUID childId, String petType, PetGrade grade) {
        return new Pet(
                UUID.randomUUID(),
                childId,
                petType,
                grade,
                0,
                PetStage.EGG,
                PetMood.IDLE,
                false,
                null,
                null
        );
    }

    public boolean addXp(int amount) {
        if (crownUnlocked) {
            return false;
        }

        PetStage previousStage = stage;
        xp += amount;
        if (stage != PetStage.AIMONG && xp >= evolutionThreshold()) {
            stage = nextStage();
            xp = 0;
        }
        return previousStage != stage;
    }

    public void unlockCrown(CrownType crownType) {
        crownUnlocked = true;
        this.crownType = crownType;
        stage = PetStage.AIMONG;
        xp = 0;
    }

    public void updateMood(PetMood mood) {
        this.mood = mood;
    }

    private int evolutionThreshold() {
        return switch (stage) {
            case EGG -> switch (grade) {
                case NORMAL -> 10;
                case RARE -> 12;
                case EPIC -> 15;
                case LEGEND -> 20;
            };
            case GROWTH -> switch (grade) {
                case NORMAL -> 30;
                case RARE -> 36;
                case EPIC -> 45;
                case LEGEND -> 60;
            };
            case AIMONG -> Integer.MAX_VALUE;
        };
    }

    private PetStage nextStage() {
        return switch (stage) {
            case EGG -> PetStage.GROWTH;
            case GROWTH, AIMONG -> PetStage.AIMONG;
        };
    }

    @PrePersist
    void prePersist() {
        if (obtainedAt == null) {
            obtainedAt = Instant.now();
        }
    }
}
