package com.aimong.backend.domain.auth.support;

import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILURE_COUNT = 3;
    private static final Duration LOCK_DURATION = Duration.ofSeconds(30);
    private static final String IP_FAIL_PREFIX = "login_fail:ip:";
    private static final String CODE_FAIL_PREFIX = "login_fail:code:";
    private static final String IP_LOCK_PREFIX = "login_lock:ip:";
    private static final String CODE_LOCK_PREFIX = "login_lock:code:";

    private final StringRedisTemplate redisTemplate;

    public void validateNotLocked(String clientIp, String code) {
        Long ipLockSeconds = redisTemplate.getExpire(ipLockKey(clientIp));
        Long codeLockSeconds = redisTemplate.getExpire(codeLockKey(code));
        long remainingSeconds = Math.max(normalizeTtl(ipLockSeconds), normalizeTtl(codeLockSeconds));

        if (remainingSeconds > 0) {
            throw new AimongException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "잠시 후 다시 시도해주세요 (" + remainingSeconds + "초 남음)"
            );
        }
    }

    public void recordFailure(String clientIp, String code) {
        registerFailure(ipFailKey(clientIp), ipLockKey(clientIp));
        registerFailure(codeFailKey(code), codeLockKey(code));
    }

    public void recordSuccess(String clientIp, String code) {
        redisTemplate.delete(ipFailKey(clientIp));
        redisTemplate.delete(codeFailKey(code));
        redisTemplate.delete(ipLockKey(clientIp));
        redisTemplate.delete(codeLockKey(code));
    }

    private void registerFailure(String failureKey, String lockKey) {
        Long failureCount = redisTemplate.opsForValue().increment(failureKey);
        if (failureCount == null) {
            return;
        }

        if (failureCount == 1L) {
            redisTemplate.expire(failureKey, LOCK_DURATION);
        }

        if (failureCount >= MAX_FAILURE_COUNT) {
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION);
            redisTemplate.delete(failureKey);
        }
    }

    private String ipFailKey(String clientIp) {
        return IP_FAIL_PREFIX + clientIp;
    }

    private String codeFailKey(String code) {
        return CODE_FAIL_PREFIX + code;
    }

    private String ipLockKey(String clientIp) {
        return IP_LOCK_PREFIX + clientIp;
    }

    private String codeLockKey(String code) {
        return CODE_LOCK_PREFIX + code;
    }

    private long normalizeTtl(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds < 0) {
            return 0;
        }
        return ttlSeconds;
    }
}
