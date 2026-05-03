package com.aimong.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "login_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttemptLimit {

    @Id
    @Column(name = "key", nullable = false, length = 255)
    private String attemptKey;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static LoginAttemptLimit firstFailure(
            String attemptKey,
            Instant expiresAt
    ) {
        LoginAttemptLimit limit = new LoginAttemptLimit();
        limit.attemptKey = attemptKey;
        limit.failureCount = 1;
        limit.expiresAt = expiresAt;
        return limit;
    }

    public void recordFailure(Instant now, Instant expiresAt, int maxFailureCount) {
        if (!this.expiresAt.isAfter(now)) {
            failureCount = 1;
            this.expiresAt = expiresAt;
            lockedUntil = null;
        } else {
            failureCount += 1;
        }

        if (failureCount >= maxFailureCount) {
            lockedUntil = expiresAt;
            failureCount = 0;
        }
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    @PrePersist
    void prePersist() {
        if (expiresAt == null) {
            expiresAt = Instant.now();
        }
    }
}
