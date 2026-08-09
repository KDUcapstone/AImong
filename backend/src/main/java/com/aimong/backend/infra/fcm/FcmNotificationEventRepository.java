package com.aimong.backend.infra.fcm;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmNotificationEventRepository extends JpaRepository<FcmNotificationEvent, UUID> {

    long countByParentIdAndStatusAndSentAtGreaterThanEqual(
            String parentId,
            FcmNotificationStatus status,
            Instant sentAt
    );

    long countByParentIdAndChildIdAndNotificationTypeAndStatusAndSentAtGreaterThanEqual(
            String parentId,
            UUID childId,
            FcmNotificationType notificationType,
            FcmNotificationStatus status,
            Instant sentAt
    );

    List<FcmNotificationEvent> findByParentIdAndNotificationTypeInAndStatusOrderByQueuedAtAsc(
            String parentId,
            Collection<FcmNotificationType> notificationTypes,
            FcmNotificationStatus status
    );

    boolean existsByParentIdAndNotificationTypeAndRefId(
            String parentId,
            FcmNotificationType notificationType,
            String refId
    );
}
