package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.LoginAttempt;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {

    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.expiresAt < :now AND (a.lockedUntil IS NULL OR a.lockedUntil < :now)")
    void deleteExpired(Instant now);
}
