package com.aimong.backend.infra.fcm;

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

@Getter
@Entity
@Table(name = "fcm_notification_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmNotificationEvent {

    @Id
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "child_id")
    private UUID childId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private FcmNotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FcmNotificationStatus status;

    @Column(name = "detected_type", length = 32)
    private String detectedType;

    @Column(name = "aggregate_count", nullable = false)
    private int aggregateCount;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static FcmNotificationEvent queuedPrivacyAlert(String parentId, UUID childId, String detectedType) {
        FcmNotificationEvent event = create(parentId, childId, FcmNotificationType.PRIVACY_ALERT, detectedType, 1);
        event.status = FcmNotificationStatus.QUEUED;
        return event;
    }

    public static FcmNotificationEvent sent(
            String parentId,
            UUID childId,
            FcmNotificationType notificationType,
            String detectedType,
            int aggregateCount
    ) {
        FcmNotificationEvent event = create(parentId, childId, notificationType, detectedType, aggregateCount);
        event.markSent();
        return event;
    }

    private static FcmNotificationEvent create(
            String parentId,
            UUID childId,
            FcmNotificationType notificationType,
            String detectedType,
            int aggregateCount
    ) {
        FcmNotificationEvent event = new FcmNotificationEvent();
        event.id = UUID.randomUUID();
        event.parentId = parentId;
        event.childId = childId;
        event.notificationType = notificationType;
        event.detectedType = detectedType;
        event.aggregateCount = aggregateCount;
        event.queuedAt = Instant.now();
        return event;
    }

    public void markSent() {
        status = FcmNotificationStatus.SENT;
        sentAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (queuedAt == null) {
            queuedAt = Instant.now();
        }
        if (status == null) {
            status = FcmNotificationStatus.QUEUED;
        }
    }
}
