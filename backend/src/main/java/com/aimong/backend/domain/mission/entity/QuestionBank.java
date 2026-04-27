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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_tags", columnDefinition = "jsonb")
    private String contentTagsJson;

    @Column(name = "curriculum_ref")
    private String curriculumRef;

    @Column(name = "difficulty")
    private Short difficulty;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_phase")
    private GenerationPhase generationPhase;

    @Column(name = "pack_no")
    private Short packNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_band", length = 16)
    private DifficultyBand difficultyBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_pool_status", length = 16)
    private QuestionPoolStatus questionPoolStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static QuestionBank create(
            UUID missionId,
            QuestionType questionType,
            String prompt,
            String optionsJson,
            String contentTagsJson,
            String curriculumRef,
            Short difficulty,
            String sourceType,
            GenerationPhase generationPhase,
            Short packNo,
            DifficultyBand difficultyBand,
            QuestionPoolStatus questionPoolStatus
    ) {
        QuestionBank questionBank = new QuestionBank();
        questionBank.id = UUID.randomUUID();
        questionBank.missionId = missionId;
        questionBank.questionType = questionType;
        questionBank.prompt = prompt;
        questionBank.optionsJson = optionsJson;
        questionBank.contentTagsJson = contentTagsJson;
        questionBank.curriculumRef = curriculumRef;
        questionBank.difficulty = difficulty;
        questionBank.sourceType = sourceType;
        questionBank.generationPhase = generationPhase;
        questionBank.packNo = packNo;
        questionBank.difficultyBand = difficultyBand;
        questionBank.questionPoolStatus = questionPoolStatus;
        questionBank.createdAt = Instant.now();
        questionBank.isActive = true;
        return questionBank;
    }

    public void quarantine() {
        this.questionPoolStatus = QuestionPoolStatus.QUARANTINED;
    }

    public boolean isQuarantined() {
        return questionPoolStatus == QuestionPoolStatus.QUARANTINED;
    }
}
