package com.aimong.backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";

    @Id
    @Column(name = "message_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content_masked", nullable = false, columnDefinition = "text")
    private String contentMasked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public static ChatMessage user(ChatSession session, String contentMasked, Instant now) {
        return create(session, ROLE_USER, contentMasked, now);
    }

    public static ChatMessage assistant(ChatSession session, String contentMasked, Instant now) {
        return create(session, ROLE_ASSISTANT, contentMasked, now);
    }

    private static ChatMessage create(ChatSession session, String role, String contentMasked, Instant now) {
        ChatMessage message = new ChatMessage();
        message.id = UUID.randomUUID();
        message.session = session;
        message.childId = session.getChildId();
        message.role = role;
        message.contentMasked = contentMasked;
        message.createdAt = now;
        return message;
    }
}
