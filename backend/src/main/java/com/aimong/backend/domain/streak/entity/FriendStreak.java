package com.aimong.backend.domain.streak.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 공동 스트릭 연결 (MVP: 1인 1파트너)
 *
 * 연결 시 A→B, B→A 두 행을 하나의 트랜잭션으로 동시 insert
 * child_id PK  → A는 파트너 1명만 가능 (DB 레벨 보장)
 * partner_child_id UNIQUE → B도 동시에 다른 파트너 불가
 * 파트너 탈퇴 시 ON DELETE CASCADE로 두 행 자동 삭제
 */
@Entity
@Table(name = "friend_streaks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FriendStreak {

    @Id
    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "child_id")
    private ChildProfile child;

    @Column(name = "partner_child_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID partnerChildId;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private OffsetDateTime connectedAt;

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) connectedAt = OffsetDateTime.now();
    }
}
