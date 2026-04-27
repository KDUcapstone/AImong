package com.aimong.backend.domain.mission.service.generation;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(QuestionCandidateGenerator.class)
public class NoopQuestionCandidateGenerator implements QuestionCandidateGenerator {

    @Override
    public List<StructuredQuestionSchema> generate(
            QuestionGenerationService.QuestionGenerationRequest request,
            String selectedModel
    ) {
        return List.of();
    }
}
