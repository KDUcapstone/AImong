package com.aimong.backend.domain.streak.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MVP: 1인 1파트너 공동 스트릭
 * A→B, B→A 대칭 2행을 하나의 트랜잭션에서 동시 insert
 */
@Entity
@Table(name = "friend_streaks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FriendStreak {

    // child_id가 PK (1인 1파트너 보장)
    @Id
    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "child_id")
    private ChildProfile child;

    // partner_child_id UNIQUE (파트너도 중복 연결 불가)
    @Column(name = "partner_child_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID partnerChildId;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private OffsetDateTime connectedAt;

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) connectedAt = OffsetDateTime.now();
    }
}
