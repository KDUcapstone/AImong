package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.pet.entity.PetGrade;
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
@Table(name = "gacha_pulls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GachaPull {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ticket_type", nullable = false)
    private TicketType ticketType;

    @Column(name = "result_pet_code", nullable = false)
    private String resultPetCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "grade", nullable = false)
    private PetGrade grade;

    @Column(name = "is_new", nullable = false)
    private boolean isNew;

    @Column(name = "granted_pet_id")
    private UUID grantedPetId;

    @Column(name = "fragments_got", nullable = false)
    private int fragmentsGot;

    @Column(name = "sr_miss_before", nullable = false)
    private int srMissBefore;

    @Column(name = "pulled_at", nullable = false)
    private Instant pulledAt;

    public static GachaPull create(
            UUID childId,
            TicketType ticketType,
            String resultPetCode,
            PetGrade grade,
            boolean isNew,
            UUID grantedPetId,
            int fragmentsGot,
            int srMissBefore
    ) {
        return new GachaPull(
                UUID.randomUUID(),
                childId,
                ticketType,
                resultPetCode,
                grade,
                isNew,
                grantedPetId,
                fragmentsGot,
                srMissBefore,
                null
        );
    }

    @PrePersist
    void prePersist() {
        if (pulledAt == null) {
            pulledAt = Instant.now();
        }
    }
}
