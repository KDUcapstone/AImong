package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuizAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QuizAttempt> findWithLockById(UUID id);
}
