package com.aimong.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "login_attempt_limits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttemptLimit {

    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private LoginAttemptTargetType targetType;

    @Column(name = "target_value", nullable = false)
    private String targetValue;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "window_expires_at", nullable = false)
    private Instant windowExpiresAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LoginAttemptLimit firstFailure(
            LoginAttemptTargetType targetType,
            String targetValue,
            Instant now,
            Instant windowExpiresAt
    ) {
        LoginAttemptLimit limit = new LoginAttemptLimit();
        limit.id = UUID.randomUUID();
        limit.targetType = targetType;
        limit.targetValue = targetValue;
        limit.failureCount = 1;
        limit.windowExpiresAt = windowExpiresAt;
        limit.updatedAt = now;
        return limit;
    }

    public void recordFailure(Instant now, Instant windowExpiresAt, int maxFailureCount) {
        if (!this.windowExpiresAt.isAfter(now)) {
            failureCount = 1;
            this.windowExpiresAt = windowExpiresAt;
            lockedUntil = null;
        } else {
            failureCount += 1;
        }

        updatedAt = now;
        if (failureCount >= maxFailureCount) {
            lockedUntil = windowExpiresAt;
            failureCount = 0;
        }
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
