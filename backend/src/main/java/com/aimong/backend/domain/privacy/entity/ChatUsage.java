package com.aimong.backend.domain.privacy.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatUsage {

    // PK: (child_id, usage_date)
    @EmbeddedId
    private ChatUsageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("childId")
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Integer count = 0;  // 0~20

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public boolean canUse() {
        return this.count < 20;
    }

    public void increment() {
        this.count++;
    }
}
