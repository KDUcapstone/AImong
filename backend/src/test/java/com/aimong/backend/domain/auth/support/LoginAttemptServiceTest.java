package com.aimong.backend.domain.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.LoginAttemptLimit;
import com.aimong.backend.domain.auth.entity.LoginAttemptTargetType;
import com.aimong.backend.domain.auth.repository.LoginAttemptLimitRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptLimitRepository loginAttemptLimitRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    void recordFailureLocksIpAndCodeAfterThirdFailure() {
        LoginAttemptLimit ipLimit = secondFailure(LoginAttemptTargetType.IP, "127.0.0.1");
        LoginAttemptLimit codeLimit = secondFailure(LoginAttemptTargetType.CODE, "000000");

        when(loginAttemptLimitRepository.findWithLockByTargetTypeAndTargetValue(
                LoginAttemptTargetType.IP,
                "127.0.0.1"
        )).thenReturn(Optional.of(ipLimit));
        when(loginAttemptLimitRepository.findWithLockByTargetTypeAndTargetValue(
                LoginAttemptTargetType.CODE,
                "000000"
        )).thenReturn(Optional.of(codeLimit));

        loginAttemptService.recordFailure("127.0.0.1", "000000");

        assertThat(ipLimit.getLockedUntil()).isAfter(Instant.now());
        assertThat(codeLimit.getLockedUntil()).isAfter(Instant.now());
        assertThat(ipLimit.getFailureCount()).isZero();
        assertThat(codeLimit.getFailureCount()).isZero();
    }

    @Test
    void validateNotLockedThrowsWhenEitherTargetIsLocked() {
        LoginAttemptLimit ipLimit = secondFailure(LoginAttemptTargetType.IP, "127.0.0.1");
        ipLimit.recordFailure(Instant.now(), Instant.now().plusSeconds(30), 3);

        when(loginAttemptLimitRepository.findByTargetTypeAndTargetValue(LoginAttemptTargetType.IP, "127.0.0.1"))
                .thenReturn(Optional.of(ipLimit));
        when(loginAttemptLimitRepository.findByTargetTypeAndTargetValue(LoginAttemptTargetType.CODE, "000000"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginAttemptService.validateNotLocked("127.0.0.1", "000000"))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void recordSuccessClearsIpAndCodeLimits() {
        loginAttemptService.recordSuccess("127.0.0.1", "482917");

        verify(loginAttemptLimitRepository).deleteByTargetTypeAndTargetValue(LoginAttemptTargetType.IP, "127.0.0.1");
        verify(loginAttemptLimitRepository).deleteByTargetTypeAndTargetValue(LoginAttemptTargetType.CODE, "482917");
    }

    private LoginAttemptLimit secondFailure(LoginAttemptTargetType targetType, String targetValue) {
        Instant now = Instant.now();
        LoginAttemptLimit limit = LoginAttemptLimit.firstFailure(targetType, targetValue, now, now.plusSeconds(60));
        limit.recordFailure(now.plusSeconds(1), now.plusSeconds(60), 3);
        return limit;
    }
}
