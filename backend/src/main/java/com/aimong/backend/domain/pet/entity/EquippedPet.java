package com.aimong.backend.domain.pet.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipped_pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EquippedPet {

    // child_id가 PK이자 child_profiles FK
    @Id
    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "child_id")
    private ChildProfile child;

    // pet_id UNIQUE — 타인 펫 장착 방지는 DB 레벨 복합 FK로 보장
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
