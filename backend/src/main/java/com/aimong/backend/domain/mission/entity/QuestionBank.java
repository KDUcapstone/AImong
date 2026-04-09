package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "question_bank")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionBank {

    @Id
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    private String optionsJson;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
