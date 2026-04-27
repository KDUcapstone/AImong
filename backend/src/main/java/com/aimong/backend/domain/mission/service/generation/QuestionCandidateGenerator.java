package com.aimong.backend.domain.mission.service.generation;

import java.util.List;

public interface QuestionCandidateGenerator {

    List<StructuredQuestionSchema> generate(
            QuestionGenerationService.QuestionGenerationRequest request,
            String selectedModel
    );
}
