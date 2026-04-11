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
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ipLockKey(clientIp)))
                || Boolean.TRUE.equals(redisTemplate.hasKey(codeLockKey(code)))) {
            throw new AimongException(ErrorCode.TOO_MANY_REQUESTS);
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
}
