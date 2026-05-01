package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "question_answer_keys", schema = "private")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAnswerKey {

    @Id
    @Column(name = "question_id", columnDefinition = "uuid", nullable = false)
    private UUID questionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "question_id")
    private QuestionBank question;

    @Column(name = "answer_payload", nullable = false, columnDefinition = "jsonb")
    private String answerPayload;

    @Column(name = "explanation", nullable = false)
    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
