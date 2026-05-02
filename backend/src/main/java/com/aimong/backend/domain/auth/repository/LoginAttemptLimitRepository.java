package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.LoginAttemptLimit;
import com.aimong.backend.domain.auth.entity.LoginAttemptTargetType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LoginAttemptLimitRepository extends JpaRepository<LoginAttemptLimit, UUID> {

    Optional<LoginAttemptLimit> findByTargetTypeAndTargetValue(LoginAttemptTargetType targetType, String targetValue);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginAttemptLimit> findWithLockByTargetTypeAndTargetValue(
            LoginAttemptTargetType targetType,
            String targetValue
    );

    @Transactional(propagation = Propagation.MANDATORY)
    void deleteByTargetTypeAndTargetValue(LoginAttemptTargetType targetType, String targetValue);
}
