package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.pet.entity.PetGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
@Table(name = "pet_fragments")
@IdClass(FragmentId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Fragment {

    @Id
    @Column(name = "child_id")
    private UUID childId;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "grade", nullable = false)
    private PetGrade grade;

    @Column(name = "count", nullable = false)
    private int count;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Fragment create(UUID childId, PetGrade grade) {
        return new Fragment(childId, grade, 0, null);
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
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
