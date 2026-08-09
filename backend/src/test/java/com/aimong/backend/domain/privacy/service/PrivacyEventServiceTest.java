package com.aimong.backend.domain.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.privacy.entity.PrivacyDetectedType;
import com.aimong.backend.domain.privacy.entity.PrivacyEvent;
import com.aimong.backend.domain.privacy.repository.PrivacyEventRepository;
import com.aimong.backend.infra.fcm.FcmNotificationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyEventServiceTest {

    @Mock private PrivacyEventRepository privacyEventRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;
    @Mock private FcmNotificationService fcmNotificationService;

    @Test
    void recordStoresOnlyDetectedTypeAndMaskedFlagThenSendsParentFcm() {
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        parent.updateFcmToken("parent-fcm-token");
        ChildProfile child = ChildProfile.create(parent, "child", "123456");
        when(childProfileRepository.findById(child.getId())).thenReturn(Optional.of(child));

        var response = service().record(child.getId(), PrivacyDetectedType.EMAIL, true);

        assertThat(response.recorded()).isTrue();
        ArgumentCaptor<PrivacyEvent> eventCaptor = ArgumentCaptor.forClass(PrivacyEvent.class);
        verify(privacyEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getChildId()).isEqualTo(child.getId());
        assertThat(eventCaptor.getValue().getDetectedType()).isEqualTo(PrivacyDetectedType.EMAIL);
        assertThat(eventCaptor.getValue().isMasked()).isTrue();
        verify(fcmNotificationService).sendPrivacyAlert(child, PrivacyDetectedType.EMAIL);
        verify(childActivityService).touchLastActiveAt(child.getId());
    }

    @Test
    void recordDelegatesFcmLimitDecisionToNotificationService() {
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        parent.updateFcmToken("parent-fcm-token");
        ChildProfile child = ChildProfile.create(parent, "child", "123456");
        when(childProfileRepository.findById(child.getId())).thenReturn(Optional.of(child));

        service().record(child.getId(), PrivacyDetectedType.PHONE, false);

        verify(privacyEventRepository).save(any(PrivacyEvent.class));
        verify(fcmNotificationService).sendPrivacyAlert(child, PrivacyDetectedType.PHONE);
    }

    private PrivacyEventService service() {
        return new PrivacyEventService(
                privacyEventRepository,
                childProfileRepository,
                childActivityService,
                fcmNotificationService
        );
    }
}
