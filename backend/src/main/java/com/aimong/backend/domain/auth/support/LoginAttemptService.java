package com.aimong.backend.domain.auth.support;

import com.aimong.backend.domain.auth.entity.LoginAttempt;
import com.aimong.backend.domain.auth.repository.LoginAttemptRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILURE_COUNT = 3;
    private static final Duration LOCK_DURATION = Duration.ofSeconds(30);
    private static final String IP_PREFIX = "login:ip:";
    private static final String CODE_PREFIX = "login:code:";

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(readOnly = true)
    public void validateNotLocked(String clientIp, String code) {
        long ipRemaining = remainingLockSeconds(ipKey(clientIp));
        long codeRemaining = remainingLockSeconds(codeKey(code));
        long remaining = Math.max(ipRemaining, codeRemaining);

        if (remaining > 0) {
            throw new AimongException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "잠시 후 다시 시도해주세요 (" + remaining + "초 남음)"
            );
        }
    }

    @Transactional
    public void recordFailure(String clientIp, String code) {
        registerFailure(ipKey(clientIp));
        registerFailure(codeKey(code));
    }

    @Transactional
    public void recordSuccess(String clientIp, String code) {
        loginAttemptRepository.deleteById(ipKey(clientIp));
        loginAttemptRepository.deleteById(codeKey(code));
    }

    private long remainingLockSeconds(String key) {
        return loginAttemptRepository.findById(key)
                .filter(a -> !a.isExpired())
                .map(LoginAttempt::remainingLockSeconds)
                .orElse(0L);
    }

    private void registerFailure(String key) {
        Instant expiresAt = Instant.now().plus(LOCK_DURATION);

        LoginAttempt attempt = loginAttemptRepository.findById(key)
                .filter(a -> !a.isExpired())
                .orElse(null);

        if (attempt == null) {
            loginAttemptRepository.save(LoginAttempt.create(key, expiresAt));
            return;
        }

        attempt.incrementFailure(expiresAt);

        if (attempt.getFailureCount() >= MAX_FAILURE_COUNT) {
            attempt.lock(expiresAt);
        }

        loginAttemptRepository.save(attempt);
    }

    private String ipKey(String clientIp) {
        return IP_PREFIX + clientIp;
    }

    private String codeKey(String code) {
        return CODE_PREFIX + code;
    }
}
