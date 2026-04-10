package com.aimong.backend.domain.pet.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 장착 중인 펫 — 자녀당 1행(child_id PK)
 * DB 레벨: FOREIGN KEY (pet_id, child_id) REFERENCES pets(id, child_id)
 * → 타인의 펫을 장착하는 것이 DB에서 원천 차단됨
 */
@Entity
@Table(name = "equipped_pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EquippedPet {

    @Id
    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "child_id")
    private ChildProfile child;

    /** 장착한 펫 ID — pets(id) 참조, UNIQUE 제약으로 같은 펫 중복 장착 불가 */
    @Column(name = "pet_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID petId;

    @Column(name = "equipped_at", nullable = false)
    private OffsetDateTime equippedAt;

    @PrePersist
    protected void onCreate() {
        if (equippedAt == null) equippedAt = OffsetDateTime.now();
    }

    public void equip(UUID petId) {
        this.petId = petId;
        this.equippedAt = OffsetDateTime.now();
    }
}
