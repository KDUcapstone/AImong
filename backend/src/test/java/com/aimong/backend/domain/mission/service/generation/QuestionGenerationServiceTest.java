package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.ModelRoutingProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                validationService(),
                new BatchDistributionValidator()
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
                List.of("AI 답을 다시 확인해야 해요"),
                List.of(),
                List.of()
        );

        StructuredQuestionSchema acceptedCandidate = new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "AI에게 개인정보를 보낼 때 가장 먼저 확인해야 할 것은 무엇일까요?",
                List.of("보내는 사람이 믿을 만한지", "파일 크기", "배경 색깔", "글자 수"),
                0,
                "개인정보를 보낼 때는 상대가 믿을 만한지 먼저 확인해야 해요.",
                List.of("PRIVACY", "SAFETY"),
                "KERIS-REF",
                2
        );
        StructuredQuestionSchema duplicateCandidate = new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "AI 답을 다시 확인해야 해요",
                List.of("보내는 사람이 믿을 만한지", "파일 크기", "배경 색깔", "글자 수"),
                0,
                "정답을 다시 확인하는 습관이 중요해요.",
                List.of("PRIVACY", "SAFETY"),
                "KERIS-REF",
                2
        );

        when(candidateGenerator.generate(request, "gpt-5-mini"))
                .thenReturn(List.of(acceptedCandidate, duplicateCandidate));

        QuestionGenerationService.GenerationBatchResult result = service.generateValidatedCandidates(request);

        assertThat(result.routingDecision().selectedModel()).isEqualTo("gpt-5-mini");
        assertThat(result.accepted()).containsExactly(acceptedCandidate);
        assertThat(result.rejected()).hasSize(1);
        assertThat(result.rejected().getFirst().report().hardFailReasons()).anyMatch(reason -> reason.contains("duplicate"));
    }

    private QuestionValidationService validationService() {
        ObjectMapper objectMapper = new ObjectMapper();
        KerisCurriculumRegistry registry = new KerisCurriculumRegistry();
        return new QuestionValidationService(
                new SchemaValidator(),
                new SafetyValidator(),
                new CurriculumFitValidator(registry),
                new StructureRuleValidator(registry),
                new ElementaryReadabilityValidator(),
                new AnswerQualityValidator(),
                new ExplanationQualityValidator(),
                new NaturalnessValidator(),
                new KoreanSurfaceLintValidator(),
                new Step3VocabularyCeilingValidator(),
                new SimilarityDeduplicator(),
                new KerisGoldExampleRegistry(objectMapper),
                objectMapper
        );
    }
}
