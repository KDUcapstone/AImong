package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "mission_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionAttempt {

    @Id
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "attempt_date", nullable = false)
    private LocalDate attemptDate;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public static MissionAttempt create(
            UUID childId,
            UUID missionId,
            LocalDate attemptDate,
            int attemptNo,
            int score,
            int total,
            int xpEarned
    ) {
        MissionAttempt attempt = new MissionAttempt();
        attempt.id = UUID.randomUUID();
        attempt.childId = childId;
        attempt.missionId = missionId;
        attempt.attemptDate = attemptDate;
        attempt.attemptNo = attemptNo;
        attempt.score = score;
        attempt.total = total;
        attempt.xpEarned = xpEarned;
        return attempt;
    }

    public boolean isReview() {
        return attemptNo > 1;
    }

    @PrePersist
    void prePersist() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }
}
