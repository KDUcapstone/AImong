package com.aimong.backend.domain.auth.support;

import com.aimong.backend.domain.auth.entity.LoginAttemptLimit;
import com.aimong.backend.domain.auth.entity.LoginAttemptTargetType;
import com.aimong.backend.domain.auth.repository.LoginAttemptLimitRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILURE_COUNT = 3;
    private static final Duration LOCK_DURATION = Duration.ofSeconds(30);

    private final LoginAttemptLimitRepository loginAttemptLimitRepository;

    @Transactional(readOnly = true)
    public void validateNotLocked(String clientIp, String code) {
        Instant now = Instant.now();
        long ipLockSeconds = remainingLockSeconds(LoginAttemptTargetType.IP, clientIp, now);
        long codeLockSeconds = remainingLockSeconds(LoginAttemptTargetType.CODE, code, now);
        long remainingSeconds = Math.max(ipLockSeconds, codeLockSeconds);

        if (remainingSeconds > 0) {
            throw new AimongException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "잠시 후 다시 시도해주세요 (" + remainingSeconds + "초 남음)"
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String clientIp, String code) {
        Instant now = Instant.now();
        registerFailure(LoginAttemptTargetType.IP, clientIp, now);
        registerFailure(LoginAttemptTargetType.CODE, code, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String clientIp, String code) {
        loginAttemptLimitRepository.deleteByAttemptKeyIfExists(attemptKey(LoginAttemptTargetType.IP, clientIp));
        loginAttemptLimitRepository.deleteByAttemptKeyIfExists(attemptKey(LoginAttemptTargetType.CODE, code));
    }

    private long remainingLockSeconds(LoginAttemptTargetType targetType, String targetValue, Instant now) {
        return loginAttemptLimitRepository.findById(attemptKey(targetType, targetValue))
                .filter(limit -> limit.isLockedAt(now))
                .map(limit -> Duration.between(now, limit.getLockedUntil()).getSeconds())
                .filter(seconds -> seconds > 0)
                .orElse(0L);
    }

    private void registerFailure(LoginAttemptTargetType targetType, String targetValue, Instant now) {
        Instant expiresAt = now.plus(LOCK_DURATION);
        String key = attemptKey(targetType, targetValue);
        LoginAttemptLimit limit = loginAttemptLimitRepository
                .findWithLockByAttemptKey(key)
                .orElse(null);

        if (limit == null) {
            loginAttemptLimitRepository.save(LoginAttemptLimit.firstFailure(key, expiresAt));
            return;
        }

        limit.recordFailure(now, expiresAt, MAX_FAILURE_COUNT);
    }

    private String attemptKey(LoginAttemptTargetType targetType, String targetValue) {
        return switch (targetType) {
            case IP -> "login:ip:" + targetValue;
            case CODE -> "login:code:" + targetValue;
        };
    }
}
