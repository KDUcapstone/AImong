package com.aimong.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Instant;
import java.util.Optional;
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
                "민수",
                "482917"
        );
        when(childProfileRepository.findById(childProfile.getId())).thenReturn(Optional.of(childProfile));

        childActivityService.touchLastActiveAt(childProfile.getId());

        assertThat(childProfile.getLastActiveAt()).isNotNull();
        assertThat(childProfile.getLastActiveAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void touchLastActiveAtThrowsWhenChildDoesNotExist() {
        UUID childId = UUID.randomUUID();
        when(childProfileRepository.findById(childId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> childActivityService.touchLastActiveAt(childId))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CHILD_NOT_FOUND);
    }
}
