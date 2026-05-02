package com.aimong.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "login_attempts")
@Getter
@NoArgsConstructor
public class LoginAttempt {

    @Id
    @Column(name = "key", length = 255)
    private String key;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static LoginAttempt create(String key, Instant expiresAt) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.key = key;
        attempt.failureCount = 1;
        attempt.expiresAt = expiresAt;
        return attempt;
    }

    public void incrementFailure(Instant expiresAt) {
        this.failureCount++;
        this.expiresAt = expiresAt;
    }

    public void lock(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
        this.failureCount = 0;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public long remainingLockSeconds() {
        if (!isLocked()) return 0;
        return lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
