package com.aimong.backend.domain.privacy.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "privacy_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivacyEvent {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "detected_type", nullable = false)
    private PrivacyDetectedType detectedType;

    @Column(name = "masked", nullable = false)
    private boolean masked;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    public static PrivacyEvent create(UUID childId, PrivacyDetectedType detectedType, boolean masked) {
        PrivacyEvent privacyEvent = new PrivacyEvent();
        privacyEvent.id = UUID.randomUUID();
        privacyEvent.childId = childId;
        privacyEvent.detectedType = detectedType;
        privacyEvent.masked = masked;
        privacyEvent.detectedAt = Instant.now();
        return privacyEvent;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
