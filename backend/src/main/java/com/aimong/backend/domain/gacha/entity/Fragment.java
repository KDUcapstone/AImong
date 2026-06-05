package com.aimong.backend.domain.gacha.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "pet_fragments",
        uniqueConstraints = @UniqueConstraint(name = "uq_pet_fragments_child", columnNames = "child_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Fragment {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "child_id")
    private UUID childId;

    @Column(name = "count", nullable = false)
    private int count;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Fragment create(UUID childId) {
        return new Fragment(UUID.randomUUID(), childId, 0, null);
    }

    public void add(int amount) {
        count += amount;
        updatedAt = Instant.now();
    }

    public boolean canSpend(int amount) {
        return count >= amount;
    }

    public void spend(int amount) {
        count -= amount;
        updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
