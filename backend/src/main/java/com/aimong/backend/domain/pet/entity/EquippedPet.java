package com.aimong.backend.domain.pet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "equipped_pets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EquippedPet {

    @Id
    @Column(name = "child_id")
    private UUID childId;

    @Column(name = "pet_id", nullable = false, unique = true)
    private UUID petId;

    @Column(name = "equipped_at", nullable = false)
    private Instant equippedAt;

    public static EquippedPet create(UUID childId, UUID petId) {
        return new EquippedPet(childId, petId, null);
    }

    public void changePet(UUID petId) {
        this.petId = petId;
        this.equippedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (equippedAt == null) {
            equippedAt = Instant.now();
        }
    }
}
