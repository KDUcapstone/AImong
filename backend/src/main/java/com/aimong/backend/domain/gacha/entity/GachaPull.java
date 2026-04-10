package com.aimong.backend.domain.gacha.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.PetGrade;
import com.aimong.backend.global.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 가챠 뽑기 기록
 * is_new=true  → granted_pet_id NOT NULL, fragments_got=0 (신규 펫 지급)
 * is_new=false → granted_pet_id NULL, fragments_got>0   (중복 → 조각 지급)
 * sr_miss_before: 이번 뽑기 직전의 srMissCount → pity 확률 계산 이력 보존
 */
@Entity
@Table(name = "gacha_pulls")
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
        if (pulledAt == null) pulledAt = OffsetDateTime.now();
    }
}
