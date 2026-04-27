package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
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
