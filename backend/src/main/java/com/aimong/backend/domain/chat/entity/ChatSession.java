package com.aimong.backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @Column(name = "session_id")
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "summary")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ChatSession() {
    }

    public static ChatSession create(UUID childId, Instant now, Duration ttl) {
        ChatSession session = new ChatSession();
        session.id = UUID.randomUUID();
        session.childId = childId;
        session.summary = null;
        session.createdAt = now;
        session.updatedAt = now;
        session.expiresAt = now.plus(ttl);
        return session;
    }

    public void refresh(Instant now, Duration ttl) {
        this.updatedAt = now;
        this.expiresAt = now.plus(ttl);
    }
}
