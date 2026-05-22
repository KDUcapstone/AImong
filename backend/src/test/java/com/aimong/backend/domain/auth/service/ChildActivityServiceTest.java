package com.aimong.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChildActivityServiceTest {

    @Mock
    private ChildProfileRepository childProfileRepository;

    @InjectMocks
    private ChildActivityService childActivityService;

    @Test
    void touchLastActiveAtUpdatesTimestamp() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "child",
                "482917"
        );
        when(childProfileRepository.touchLastActiveAtIfDue(eq(childProfile.getId()), any(), any())).thenReturn(1);

        childActivityService.touchLastActiveAt(childProfile.getId());

        verify(childProfileRepository).touchLastActiveAtIfDue(eq(childProfile.getId()), any(), any());
    }

    @Test
    void touchLastActiveAtSkipsRecentActivityWrite() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "child",
                "482917"
        );
        when(childProfileRepository.touchLastActiveAtIfDue(eq(childProfile.getId()), any(), any())).thenReturn(1);

        childActivityService.touchLastActiveAt(childProfile.getId());
        childActivityService.touchLastActiveAt(childProfile.getId());

        verify(childProfileRepository, times(1)).touchLastActiveAtIfDue(eq(childProfile.getId()), any(), any());
    }

    @Test
    void touchLastActiveAtThrowsWhenChildDoesNotExist() {
        UUID childId = UUID.randomUUID();
        when(childProfileRepository.touchLastActiveAtIfDue(eq(childId), any(), any())).thenReturn(0);
        when(childProfileRepository.existsByIdAndDeletedAtIsNull(childId)).thenReturn(false);

        assertThatThrownBy(() -> childActivityService.touchLastActiveAt(childId))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CHILD_NOT_FOUND);
    }
}
