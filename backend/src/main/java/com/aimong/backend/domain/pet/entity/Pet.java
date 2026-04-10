package com.aimong.backend.domain.pet.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.CrownType;
import com.aimong.backend.global.enums.PetGrade;
import com.aimong.backend.global.enums.PetMood;
import com.aimong.backend.global.enums.PetStage;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pets")
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

    /** 펫 종류 코드 (ex: "cat_01", "dragon_02") */
    @Column(name = "pet_type", nullable = false)
    private String petType;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, columnDefinition = "pet_grade_enum")
    private PetGrade grade;

    @Column(name = "xp", nullable = false)
    @Builder.Default
    private Integer xp = 0;

    /**
     * 성장 단계: EGG(0~79) / GROWTH(80~249) / AIMONG(250+)
     * 미션 XP 획득 시 자동 업데이트
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, columnDefinition = "pet_stage_enum")
    @Builder.Default
    private PetStage stage = PetStage.EGG;

    /**
     * 감정 상태: HAPPY(오늘 미션 완료) / IDLE(미완료) / SAD_LIGHT(1일) / SAD_DEEP(2일+)
     * 스케줄러가 매일 00:01 KST에 업데이트
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mood", nullable = false, columnDefinition = "pet_mood_enum")
    @Builder.Default
    private PetMood mood = PetMood.IDLE;

    /** 아이몽(AIMONG 단계) 달성 시 해금되는 영구 왕관 */
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
        if (obtainedAt == null) obtainedAt = OffsetDateTime.now();
    }

    public void addXp(int amount) {
        this.xp += amount;
        updateStage();
    }

    private void updateStage() {
        if (this.xp >= 250) this.stage = PetStage.AIMONG;
        else if (this.xp >= 80) this.stage = PetStage.GROWTH;
        else this.stage = PetStage.EGG;
    }

    public void updateMood(PetMood mood) {
        this.mood = mood;
    }

    public void unlockCrown(CrownType crownType) {
        this.crownUnlocked = true;
        this.crownType = crownType;
    }
}
