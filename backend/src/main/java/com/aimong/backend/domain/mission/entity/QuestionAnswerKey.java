package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "question_answer_keys", schema = "private")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionAnswerKey {

    @Id
    @Column(name = "question_id")
    private UUID questionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_payload", nullable = false, columnDefinition = "jsonb")
    private String answerPayload;

    @Column(name = "explanation", nullable = false)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
