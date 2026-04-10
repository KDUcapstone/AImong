package com.aimong.backend.domain.mission.entity;

import com.aimong.backend.global.enums.QuestionSource;
import com.aimong.backend.global.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문제 본문 + 보기만 저장 (공개 스키마)
 * 정답/해설은 private.question_answer_keys에 분리 저장 — 클라이언트 접근 불가
 */
@Entity
@Table(name = "question_bank")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    /** OX / MULTIPLE(객관식) / FILL(빈칸) / SITUATION(상황판단) */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, columnDefinition = "question_type_enum")
    private QuestionType questionType;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    /** JSONB: 보기 배열 ex) ["①AI는...", "②딥러닝은..."] — OX/FILL은 null */
    @Column(name = "options", columnDefinition = "jsonb")
    private String options;

    /** STATIC(사전 제작) / GPT(실시간 생성) / FALLBACK(GPT 실패 시 대체) */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "question_source_enum")
    @Builder.Default
    private QuestionSource sourceType = QuestionSource.STATIC;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
