package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * private.question_answer_keys — 정답/해설 (비공개 스키마, 클라이언트 직접 접근 불가)
 * Spring Boot에서는 private 스키마를 명시해서 접근
 */
@Entity
@Table(name = "question_answer_keys", schema = "private")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuestionAnswerKey {

    // question_bank.id와 1:1 관계, PK = FK
    @Id
    @Column(name = "question_id", columnDefinition = "uuid", nullable = false)
    private UUID questionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "question_id")
    private QuestionBank question;

    // JSONB: 정답 데이터 ex) {"answer": "①", "index": 0}
    @Column(name = "answer_payload", nullable = false, columnDefinition = "jsonb")
    private String answerPayload;

    @Column(name = "explanation", nullable = false)
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
