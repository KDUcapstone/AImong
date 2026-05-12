package com.aimong.backend.domain.chat.repository;

import com.aimong.backend.domain.chat.entity.ChatSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByIdAndChildIdAndExpiresAtAfter(UUID id, UUID childId, Instant now);

    Optional<ChatSession> findFirstByChildIdAndExpiresAtAfterOrderByUpdatedAtDesc(UUID childId, Instant now);

    int deleteByExpiresAtBefore(Instant now);
}
