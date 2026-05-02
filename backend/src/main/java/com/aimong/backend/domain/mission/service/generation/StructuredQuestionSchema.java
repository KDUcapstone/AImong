package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;

public record StructuredQuestionSchema(
        String missionCode,
        int packNo,
        DifficultyBand difficultyBand,
        QuestionType type,
        String question,
        List<String> options,
        Object answer,
        String explanation,
        List<String> contentTags,
        String curriculumRef,
        int difficulty,
        DifficultyBand canonicalDifficulty
) {

    public StructuredQuestionSchema(
            String missionCode,
            int packNo,
            DifficultyBand difficultyBand,
            QuestionType type,
            String question,
            List<String> options,
            Object answer,
            String explanation,
            List<String> contentTags,
            String curriculumRef,
            int difficulty
    ) {
        this(
                missionCode,
                packNo,
                difficultyBand,
                type,
                question,
                options,
                answer,
                explanation,
                contentTags,
                curriculumRef,
                difficulty,
                difficultyBand
        );
    }

    public DifficultyBand effectiveDifficulty() {
        return canonicalDifficulty != null ? canonicalDifficulty : difficultyBand;
    }
}
