package com.aimong.backend.domain.streak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "friend_streaks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FriendStreak {

    @Id
    @Column(name = "child_id")
    private UUID childId;

    @Column(name = "partner_child_id", nullable = false, unique = true)
    private UUID partnerChildId;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    public static FriendStreak create(UUID childId, UUID partnerChildId) {
        return new FriendStreak(childId, partnerChildId, Instant.now());
    }
}
