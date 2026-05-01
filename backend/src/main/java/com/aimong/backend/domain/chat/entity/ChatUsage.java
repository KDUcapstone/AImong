package com.aimong.backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_usage")
@IdClass(ChatUsageId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatUsage {

    @Id
    @Column(name = "child_id")
    private UUID childId;

    @Id
    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Column(name = "count", nullable = false)
    private int count;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ChatUsage create(UUID childId, LocalDate usageDate) {
        ChatUsage chatUsage = new ChatUsage();
        chatUsage.childId = childId;
        chatUsage.usageDate = usageDate;
        chatUsage.count = 0;
        chatUsage.updatedAt = Instant.now();
        return chatUsage;
    }

    public void increment() {
        count += 1;
        updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
