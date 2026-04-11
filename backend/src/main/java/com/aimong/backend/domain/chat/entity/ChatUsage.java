package com.aimong.backend.domain.chat.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.OffsetDateTime;

/**
 * 챗봇 일일 사용량.
 * child_id + usage_date 기준으로 하루 최대 20회 제한을 추적한다.
 */
@Entity
@Table(name = "chat_usage")
@Check(constraints = "count BETWEEN 0 AND 20")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatUsage {

    @EmbeddedId
    private ChatUsageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("childId")
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Integer count = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void increment() {
        this.count++;
    }
}
