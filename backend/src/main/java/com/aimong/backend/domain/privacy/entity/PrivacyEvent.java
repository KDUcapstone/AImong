package com.aimong.backend.domain.privacy.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.PrivacyDetectedType;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 개인정보 감지 이벤트
 * ⚠️ 원문 텍스트는 절대 저장하지 않음 — 감지 유형과 마스킹 여부만 기록
 * 3-tier 감지: ML Kit → Regex/키워드 규칙 → 서버 필터
 */
@Entity
@Table(name = "privacy_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrivacyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    /** NAME / SCHOOL / AGE / PHONE / EMAIL / ADDRESS / DATE / URL / ETC */
    @Enumerated(EnumType.STRING)
    @Column(name = "detected_type", nullable = false, columnDefinition = "privacy_detected_type_enum")
    private PrivacyDetectedType detectedType;

    /** 마스킹 처리 여부 */
    @Column(name = "masked", nullable = false)
    private Boolean masked;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private OffsetDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) detectedAt = OffsetDateTime.now();
    }
}
