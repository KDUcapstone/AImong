package com.aimong.backend.domain.mission.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문제 세션 (API v1.1)
 * GET /missions/{missionId}/questions 호출 시 생성
 * POST /submit 시 submitted_at 업데이트 → 만료 전 1회만 제출 가능
 */
@Entity
@Table(name = "quiz_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizAttempt {

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

    /** JSONB: 배정된 문제 5개 UUID 배열 ex) ["uuid1","uuid2",...] */
    @Column(name = "question_ids_json", nullable = false, columnDefinition = "jsonb")
    private String questionIdsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 권장 30분 — 초과 시 API에서 400 반환 */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** null이면 미제출 */
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public void submit() {
        this.submittedAt = OffsetDateTime.now();
    }
}
