package com.aimong.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.dto.ChildLoginRequest;
import com.aimong.backend.domain.auth.dto.ChildLoginResponse;
import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.support.LoginAttemptService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChildAuthServiceTest {

    @Mock
    private ChildProfileRepository childProfileRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private ChildActivityService childActivityService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ChildAuthService childAuthService;

    @Test
    void loginReturnsSessionPayloadAndUpdatesLastActiveAt() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "민준",
                "482917"
        );
        childProfile.applyMissionXp(250, java.time.LocalDate.of(2026, 4, 13), java.time.LocalDate.of(2026, 4, 13));

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(childProfileRepository.findByCode("482917")).thenReturn(Optional.of(childProfile));
        when(jwtProvider.createChildSessionToken(childProfile.getId().toString(), childProfile.getSessionVersion()))
                .thenReturn("jwt-token");

        ChildLoginResponse response = childAuthService.login(new ChildLoginRequest("482917"), httpServletRequest);

        assertThat(response.childId()).isEqualTo(childProfile.getId());
        assertThat(response.nickname()).isEqualTo("민준");
        assertThat(response.sessionToken()).isEqualTo("jwt-token");
        assertThat(response.profileImageType()).isEqualTo(childProfile.getProfileImageType().name());
        assertThat(response.totalXp()).isEqualTo(250);
        assertThat(childProfile.getLastActiveAt()).isNotNull();
        assertThat(childProfile.getLastActiveAt()).isBeforeOrEqualTo(Instant.now());
        verify(loginAttemptService).validateNotLocked("127.0.0.1", "482917");
        verify(loginAttemptService).recordSuccess("127.0.0.1", "482917");
    }

    @Test
    void loginRecordsFailureWhenChildCodeDoesNotExist() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(childProfileRepository.findByCode("000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> childAuthService.login(new ChildLoginRequest("000000"), httpServletRequest))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CHILD_CODE_NOT_FOUND);

        verify(loginAttemptService).validateNotLocked("127.0.0.1", "000000");
        verify(loginAttemptService).recordFailure("127.0.0.1", "000000");
    }

    @Test
    void loginRecordsFailureWhenCodeFormatIsInvalid() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatThrownBy(() -> childAuthService.login(new ChildLoginRequest("abc12"), httpServletRequest))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CHILD_CODE_INVALID_FORMAT);

        verify(loginAttemptService).validateNotLocked("127.0.0.1", "abc12");
        verify(loginAttemptService).recordFailure("127.0.0.1", "abc12");
    }

    @Test
    void loginRecordsFailureWhenCodeIsBlank() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThatThrownBy(() -> childAuthService.login(new ChildLoginRequest(" "), httpServletRequest))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CHILD_CODE_REQUIRED);

        verify(loginAttemptService).validateNotLocked("127.0.0.1", "<blank>");
        verify(loginAttemptService).recordFailure("127.0.0.1", "<blank>");
    }
}
