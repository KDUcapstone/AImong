package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.PetGrade;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * 조각 보유량 — PK: (child_id, grade)
 * 중복 펫 뽑기 시 조각 지급: NORMAL+1 / RARE+3 / EPIC+8 / LEGEND+20
 * 교환 기준: NORMAL 10개→펫1 / RARE 30개 / EPIC 80개 / LEGEND 200개
 */
@Entity
@Table(name = "fragments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Fragment {

    @EmbeddedId
    private FragmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("childId")
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Integer count = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void add(int amount) {
        this.count += amount;
    }

    public PetGrade getGrade() {
        return id.getGrade();
    }
}
