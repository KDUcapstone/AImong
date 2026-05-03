package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.LoginAttemptLimit;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LoginAttemptLimitRepository extends JpaRepository<LoginAttemptLimit, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginAttemptLimit> findWithLockByAttemptKey(String attemptKey);

    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query("delete from LoginAttemptLimit l where l.attemptKey = :attemptKey")
    void deleteByAttemptKeyIfExists(String attemptKey);
}
