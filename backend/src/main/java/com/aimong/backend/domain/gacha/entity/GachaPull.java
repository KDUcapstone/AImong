package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.PetGrade;
import com.aimong.backend.global.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "gacha_pulls",
    indexes = {
        @Index(name = "idx_gacha_pulls_child", columnList = "child_id, pulled_at")
    }
)
@Check(constraints = "(is_new = true AND granted_pet_id IS NOT NULL AND fragments_got = 0) OR (is_new = false AND granted_pet_id IS NULL AND fragments_got > 0)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GachaPull {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, columnDefinition = "ticket_type_enum")
    private TicketType ticketType;

    @Column(name = "result_pet_code", nullable = false)
    private String resultPetCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, columnDefinition = "pet_grade_enum")
    private PetGrade grade;

    @Column(name = "is_new", nullable = false)
    private Boolean isNew;

    @Column(name = "granted_pet_id", columnDefinition = "uuid")
    private UUID grantedPetId;

    @Column(name = "fragments_got", nullable = false)
    @Builder.Default
    private Integer fragmentsGot = 0;

    @Column(name = "sr_miss_before", nullable = false)
    @Builder.Default
    private Integer srMissBefore = 0;

    @Column(name = "pulled_at", nullable = false, updatable = false)
    private OffsetDateTime pulledAt;

    @PrePersist
    protected void onCreate() {
        if (pulledAt == null) {
            pulledAt = OffsetDateTime.now();
        }
    }
}
