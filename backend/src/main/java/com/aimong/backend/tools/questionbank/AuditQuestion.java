package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.service.generation.StructuredQuestionSchema;
import java.util.List;

public record AuditQuestion(
        String externalId,
        String missionCode,
        short stage,
        String stageTitle,
        String missionTitle,
        QuestionType type,
        String question,
        List<String> options,
        Object answer,
        String explanation,
        List<String> contentTags,
        String curriculumRef,
        int difficulty,
        DifficultyBand difficultyBand,
        Integer packNo,
        String sourceType
) {
    public StructuredQuestionSchema toStructuredQuestionSchema() {
        return new StructuredQuestionSchema(
                missionCode,
                packNo == null ? 0 : packNo,
                difficultyBand,
                type,
                question,
                options,
                answer,
                explanation,
                contentTags,
                curriculumRef,
                difficulty
        );
    }
}
