package com.aimong.backend.domain.chat.repository;

import com.aimong.backend.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop10BySession_IdOrderByCreatedAtDesc(UUID sessionId);
}
