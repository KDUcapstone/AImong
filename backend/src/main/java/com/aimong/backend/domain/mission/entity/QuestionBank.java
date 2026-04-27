package com.aimong.backend.domain.mission.entity;

import com.aimong.backend.global.enums.QuestionDifficulty;
import com.aimong.backend.global.enums.QuestionGenerationPhase;
import com.aimong.backend.global.enums.QuestionSource;
import com.aimong.backend.global.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, columnDefinition = "question_type_enum")
    private QuestionType questionType;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "options", columnDefinition = "jsonb")
    private String options;

    @Column(name = "content_tags", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String contentTags = "[]";

    @Column(name = "curriculum_ref", nullable = false)
    private String curriculumRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, columnDefinition = "question_difficulty_enum")
    private QuestionDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "question_source_enum")
    @Builder.Default
    private QuestionSource sourceType = QuestionSource.STATIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_phase", nullable = false, columnDefinition = "question_generation_phase_enum")
    @Builder.Default
    private QuestionGenerationPhase generationPhase = QuestionGenerationPhase.PREGENERATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
