package com.aimong.backend.domain.progress.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.mission.entity.Mission;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mission_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MissionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "attempt_date", nullable = false)
    private LocalDate attemptDate;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;  // 당일 몇 번째 시도 (1부터 시작)

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned;

    // DB GENERATED ALWAYS AS (attempt_no > 1) STORED
    @Column(name = "is_review", insertable = false, updatable = false)
    private Boolean isReview;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) submittedAt = OffsetDateTime.now();
    }
}
