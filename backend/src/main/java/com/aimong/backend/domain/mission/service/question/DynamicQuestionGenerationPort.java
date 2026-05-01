package com.aimong.backend.domain.mission.service.question.postmvp;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.service.question.RecompositionSelector;
import java.util.List;
import java.util.UUID;

public interface DynamicQuestionGenerationPort {

    List<QuestionBank> generateQuestions(
            UUID missionId,
            RecompositionSelector.ShortageDetails shortageDetails,
            UUID childId,
            boolean isReview
    );
}
