package com.aimong.backend.domain.gacha.entity;

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
@Table(name = "tickets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Ticket {

    @Id
    @Column(name = "child_id")
    private UUID childId;

    @Column(name = "normal", nullable = false)
    private int normal;

    @Column(name = "rare", nullable = false)
    private int rare;

    @Column(name = "epic", nullable = false)
    private int epic;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Ticket create(UUID childId, int normal) {
        return new Ticket(childId, normal, 0, 0, null);
    }

    public void addNormal(int count) {
        normal += count;
        updatedAt = Instant.now();
    }

    public void addRare(int count) {
        rare += count;
        updatedAt = Instant.now();
    }

    public void addEpic(int count) {
        epic += count;
        updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
