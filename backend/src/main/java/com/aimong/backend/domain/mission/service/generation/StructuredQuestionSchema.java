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
        int difficulty
) {
}
