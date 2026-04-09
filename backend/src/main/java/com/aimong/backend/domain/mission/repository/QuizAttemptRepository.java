package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.QuizAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByIdAndChildId(UUID id, UUID childId);
}
