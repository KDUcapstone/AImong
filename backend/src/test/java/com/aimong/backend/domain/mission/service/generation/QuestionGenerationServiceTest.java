package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.ModelRoutingProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock
    private QuestionCandidateGenerator candidateGenerator;

    @Test
    void filtersInvalidAndDuplicateCandidates() {
        QuestionGenerationService service = new QuestionGenerationService(
                new KerisCurriculumRegistry(),
                new ModelRoutingPolicy(new ModelRoutingProperties("gpt-5-mini", "gpt-5.4-mini")),
                candidateGenerator,
                new QuestionValidator(new KerisCurriculumRegistry(), new ChildSafetyFilter()),
                new SimilarityDeduplicator()
        );

        QuestionGenerationService.QuestionGenerationRequest request = new QuestionGenerationService.QuestionGenerationRequest(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                1,
                2,
                0,
                false,
                false,
                false,
                false,
                List.of("AI 답은 다시 확인해야 해요"),
                List.of()
        );

        StructuredQuestionSchema acceptedCandidate = new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "AI에게 도움을 받을 때 가장 안전한 입력은 무엇일까요?",
                List.of("가상의 이름", "좋아하는 색", "발표 주제", "원하는 형식"),
                0,
                "실제 개인정보 대신 가상의 예시를 쓰는 것이 안전해요.",
                List.of("PRIVACY", "SAFETY"),
                "KERIS-1 Ch2.1 pp.27-29; Ch4.2 pp.163-178; D0qG389 STEP 2",
                2
        );
        StructuredQuestionSchema duplicateCandidate = new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "AI 답은 다시 확인해야 해요",
                List.of("가상의 이름", "좋아하는 색", "발표 주제", "원하는 형식"),
                0,
                "실제 개인정보 대신 가상의 예시를 쓰는 것이 안전해요.",
                List.of("PRIVACY", "SAFETY"),
                "KERIS-1 Ch2.1 pp.27-29; Ch4.2 pp.163-178; D0qG389 STEP 2",
                2
        );

        when(candidateGenerator.generate(request, "gpt-5-mini"))
                .thenReturn(List.of(acceptedCandidate, duplicateCandidate));

        QuestionGenerationService.GenerationBatchResult result = service.generateValidatedCandidates(request);

        assertThat(result.routingDecision().selectedModel()).isEqualTo("gpt-5-mini");
        assertThat(result.accepted()).containsExactly(acceptedCandidate);
        assertThat(result.rejected()).hasSize(1);
        assertThat(result.rejected().get(0).reasons()).anyMatch(reason -> reason.contains("duplicate"));
    }
}
