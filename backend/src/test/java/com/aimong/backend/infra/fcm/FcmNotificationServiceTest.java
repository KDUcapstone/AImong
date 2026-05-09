package com.aimong.backend.infra.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.privacy.entity.PrivacyDetectedType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FcmNotificationServiceTest {

    @Mock private FcmService fcmService;
    @Mock private FcmNotificationEventRepository fcmNotificationEventRepository;

    @Test
    void sendPrivacyAlertQueuesWhenParentDailyLimitIsReached() {
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        parent.updateFcmToken("parent-fcm-token");
        ChildProfile child = ChildProfile.create(parent, "child", "123456");
        when(fcmNotificationEventRepository.countByParentIdAndStatusAndSentAtGreaterThanEqual(
                eq("parent-id"),
                eq(FcmNotificationStatus.SENT),
                any(Instant.class)
        )).thenReturn(5L);

        service().sendPrivacyAlert(child, PrivacyDetectedType.EMAIL);

        ArgumentCaptor<FcmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(FcmNotificationEvent.class);
        verify(fcmNotificationEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(FcmNotificationStatus.QUEUED);
        assertThat(eventCaptor.getValue().getNotificationType()).isEqualTo(FcmNotificationType.PRIVACY_ALERT);
        verify(fcmService, never()).sendToToken(any(), any());
    }

    @Test
    void sendPrivacyAlertFlushesQueuedAlertsAsBatchBeforeCurrentAlert() {
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        parent.updateFcmToken("parent-fcm-token");
        ChildProfile child = ChildProfile.create(parent, "child", "123456");
        FcmNotificationEvent queued = FcmNotificationEvent.queuedPrivacyAlert(
                "parent-id",
                child.getId(),
                PrivacyDetectedType.PHONE.name()
        );
        when(fcmNotificationEventRepository.countByParentIdAndStatusAndSentAtGreaterThanEqual(
                eq("parent-id"),
                eq(FcmNotificationStatus.SENT),
                any(Instant.class)
        )).thenReturn(0L);
        when(fcmNotificationEventRepository.findByParentIdAndNotificationTypeInAndStatusOrderByQueuedAtAsc(
                eq("parent-id"),
                eq(List.of(FcmNotificationType.PRIVACY_ALERT)),
                eq(FcmNotificationStatus.QUEUED)
        )).thenReturn(List.of(queued));
        when(fcmNotificationEventRepository.countByParentIdAndChildIdAndNotificationTypeAndStatusAndSentAtGreaterThanEqual(
                eq("parent-id"),
                eq(child.getId()),
                eq(FcmNotificationType.PRIVACY_ALERT),
                eq(FcmNotificationStatus.SENT),
                any(Instant.class)
        )).thenReturn(0L);

        service().sendPrivacyAlert(child, PrivacyDetectedType.EMAIL);

        assertThat(queued.getStatus()).isEqualTo(FcmNotificationStatus.SENT);
        verify(fcmService).sendToToken(eq("parent-fcm-token"), org.mockito.ArgumentMatchers.argThat(
                payload -> FcmNotificationType.PRIVACY_ALERT_BATCH.name().equals(payload.data().get("type"))
        ));
        verify(fcmService).sendToToken(eq("parent-fcm-token"), org.mockito.ArgumentMatchers.argThat(
                payload -> FcmNotificationType.PRIVACY_ALERT.name().equals(payload.data().get("type"))
        ));
    }

    private FcmNotificationService service() {
        return new FcmNotificationService(fcmService, fcmNotificationEventRepository);
    }
}
