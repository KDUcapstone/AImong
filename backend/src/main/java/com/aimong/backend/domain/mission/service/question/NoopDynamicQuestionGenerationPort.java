package com.aimong.backend.domain.mission.service.question.postmvp;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.service.question.RecompositionSelector;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
// Post-MVP fallback bean. MVP runtime does not invoke this port.
public class NoopDynamicQuestionGenerationPort implements DynamicQuestionGenerationPort {

    @Override
    public List<QuestionBank> generateQuestions(
            UUID missionId,
            RecompositionSelector.ShortageDetails shortageDetails,
            UUID childId,
            boolean isReview
    ) {
        return List.of();
    }
}
