package com.aimong.backend.domain.mission.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 미션 제출 1건당 1행을 저장하는 시도 로그.
 * 당일 첫 제출은 일반 모드, 이후 제출은 복습 모드로 취급한다.
 */
@Entity
@Table(
    name = "mission_attempts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_mission_attempts_child_mission_date_no",
            columnNames = {"child_id", "mission_id", "attempt_date", "attempt_no"}
        ),
        @UniqueConstraint(name = "uk_mission_attempts_idempotency_key", columnNames = "idempotency_key")
    },
    indexes = {
        @Index(name = "idx_mission_attempts_child_date", columnList = "child_id, attempt_date")
    }
)
@Check(constraints = "score <= total")
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
    private Integer attemptNo;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned;

    /**
     * DB generated column: attempt_no > 1 이면 true.
     * 애플리케이션에서는 읽기 전용으로만 사용한다.
     */
    @Column(name = "is_review", insertable = false, updatable = false)
    private Boolean isReview;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = OffsetDateTime.now();
        }
    }
}
