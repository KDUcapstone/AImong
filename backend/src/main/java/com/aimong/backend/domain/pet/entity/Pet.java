package com.aimong.backend.domain.pet.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.CrownType;
import com.aimong.backend.global.enums.PetGrade;
import com.aimong.backend.global.enums.PetMood;
import com.aimong.backend.global.enums.PetStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "pets",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pets_child_pet_type", columnNames = {"child_id", "pet_type"})
    }
)
@Check(constraints = "(crown_unlocked = false AND crown_type IS NULL) OR (crown_unlocked = true AND crown_type IS NOT NULL)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "pet_type", nullable = false)
    private String petType;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, columnDefinition = "pet_grade_enum")
    private PetGrade grade;

    @Column(name = "xp", nullable = false)
    @Builder.Default
    private Integer xp = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, columnDefinition = "pet_stage_enum")
    @Builder.Default
    private PetStage stage = PetStage.EGG;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood", nullable = false, columnDefinition = "pet_mood_enum")
    @Builder.Default
    private PetMood mood = PetMood.IDLE;

    @Column(name = "crown_unlocked", nullable = false)
    @Builder.Default
    private Boolean crownUnlocked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "crown_type", columnDefinition = "crown_type_enum")
    private CrownType crownType;

    @Column(name = "obtained_at", nullable = false, updatable = false)
    private OffsetDateTime obtainedAt;

    @PrePersist
    protected void onCreate() {
        if (obtainedAt == null) {
            obtainedAt = OffsetDateTime.now();
        }
    }

    public void addXp(int amount) {
        this.xp += amount;
        updateStage();
    }

    private void updateStage() {
        if (this.xp >= 250) {
            this.stage = PetStage.AIMONG;
        } else if (this.xp >= 80) {
            this.stage = PetStage.GROWTH;
        } else {
            this.stage = PetStage.EGG;
        }
    }

    public void updateMood(PetMood mood) {
        this.mood = mood;
    }

    public void unlockCrown(CrownType crownType) {
        this.crownUnlocked = true;
        this.crownType = crownType;
    }
}
