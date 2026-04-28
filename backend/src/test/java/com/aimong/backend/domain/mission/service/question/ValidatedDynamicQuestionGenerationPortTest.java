package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.service.generation.AnswerQualityValidator;
import com.aimong.backend.domain.mission.service.generation.CurriculumFitValidator;
import com.aimong.backend.domain.mission.service.generation.ElementaryReadabilityValidator;
import com.aimong.backend.domain.mission.service.generation.ExplanationQualityValidator;
import com.aimong.backend.domain.mission.service.generation.GeneratedQuestionPersistenceService;
import com.aimong.backend.domain.mission.service.generation.KerisCurriculumRegistry;
import com.aimong.backend.domain.mission.service.generation.KerisGoldExampleRegistry;
import com.aimong.backend.domain.mission.service.generation.KoreanSurfaceLintValidator;
import com.aimong.backend.domain.mission.service.generation.MissionCodeResolver;
import com.aimong.backend.domain.mission.service.generation.NaturalnessValidator;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationReport;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationScores;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationService;
import com.aimong.backend.domain.mission.service.generation.SafetyValidator;
import com.aimong.backend.domain.mission.service.generation.SchemaValidator;
import com.aimong.backend.domain.mission.service.generation.SimilarityDeduplicator;
import com.aimong.backend.domain.mission.service.generation.Step3VocabularyCeilingValidator;
import com.aimong.backend.domain.mission.service.generation.StructureRuleValidator;
import com.aimong.backend.domain.mission.service.generation.StructuredQuestionSchema;
import com.aimong.backend.domain.mission.service.generation.ValidationDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidatedDynamicQuestionGenerationPortTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private QuestionAnswerKeyRepository questionAnswerKeyRepository;

    @Mock
    private QuestionGenerationService questionGenerationService;

    @Mock
    private MissionCodeResolver missionCodeResolver;

    @Test
    void refillRequestsReflectBandShortageAndTypePreference() {
        ValidatedDynamicQuestionGenerationPort port = port(permissiveValidationService());

        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(mission.getStage()).thenReturn((short) 2);
        when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of("S0203"));
        when(questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId)).thenReturn(List.of());
        when(questionGenerationService.generateValidatedCandidates(any())).thenAnswer(invocation -> {
            QuestionGenerationService.QuestionGenerationRequest request = invocation.getArgument(0);
            return new QuestionGenerationService.GenerationBatchResult(
                    new com.aimong.backend.domain.mission.service.generation.ModelRoutingPolicy.RoutingDecision("gpt-5-mini", false),
                    List.of(candidate(request.missionCode(), request.difficultyBand(), request.desiredType(), request.numericDifficulty())),
                    List.of()
            );
        });
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionAnswerKeyRepository.save(any(QuestionAnswerKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<QuestionBank> generated = port.generateQuestions(
                missionId,
                new RecompositionSelector.ShortageDetails(
                        1,
                        1,
                        1,
                        0,
                        "INSUFFICIENT_DIFFICULTY_BAND_POOL",
                        new RecompositionSelector.CandidatePoolCounts(7, 4, 2, 1)
                ),
                UUID.randomUUID(),
                false
        );

        assertThat(generated).hasSize(3);

        ArgumentCaptor<QuestionGenerationService.QuestionGenerationRequest> captor =
                ArgumentCaptor.forClass(QuestionGenerationService.QuestionGenerationRequest.class);
        verify(questionGenerationService, org.mockito.Mockito.times(3)).generateValidatedCandidates(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(QuestionGenerationService.QuestionGenerationRequest::difficultyBand)
                .containsExactly(DifficultyBand.LOW, DifficultyBand.MEDIUM, DifficultyBand.HIGH);
        assertThat(captor.getAllValues())
                .extracting(QuestionGenerationService.QuestionGenerationRequest::desiredType)
                .containsExactly(QuestionType.OX, QuestionType.MULTIPLE, QuestionType.MULTIPLE);
    }

    @Test
    void validationFailedQuestionIsNotSaved() {
        ValidatedDynamicQuestionGenerationPort port = port(rejectingValidationService());

        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(mission.getStage()).thenReturn((short) 2);
        when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of("S0203"));
        when(questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId)).thenReturn(List.of());
        when(questionGenerationService.generateValidatedCandidates(any())).thenAnswer(invocation -> {
            QuestionGenerationService.QuestionGenerationRequest request = invocation.getArgument(0);
            return new QuestionGenerationService.GenerationBatchResult(
                    new com.aimong.backend.domain.mission.service.generation.ModelRoutingPolicy.RoutingDecision("gpt-5-mini", false),
                    List.of(candidate(request.missionCode(), request.difficultyBand(), request.desiredType(), request.numericDifficulty())),
                    List.of()
            );
        });

        List<QuestionBank> generated = port.generateQuestions(
                missionId,
                new RecompositionSelector.ShortageDetails(
                        1,
                        0,
                        0,
                        0,
                        "INSUFFICIENT_DIFFICULTY_BAND_POOL",
                        new RecompositionSelector.CandidatePoolCounts(9, 4, 3, 2)
                ),
                UUID.randomUUID(),
                false
        );

        assertThat(generated).isEmpty();
        verify(questionBankRepository, org.mockito.Mockito.never()).save(any(QuestionBank.class));
        verify(questionAnswerKeyRepository, org.mockito.Mockito.never()).save(any(QuestionAnswerKey.class));
    }

    @Test
    void retryFeedbackIsPropagatedToFollowUpGenerationRequest() {
        ValidatedDynamicQuestionGenerationPort port = port(permissiveValidationService());

        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(mission.getStage()).thenReturn((short) 2);
        when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of("S0203"));
        when(questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId)).thenReturn(List.of());
        when(questionBankRepository.save(any(QuestionBank.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionAnswerKeyRepository.save(any(QuestionAnswerKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionGenerationService.generateValidatedCandidates(any())).thenAnswer(invocation -> {
            QuestionGenerationService.QuestionGenerationRequest request = invocation.getArgument(0);
            if (request.validationFailureCount() == 0) {
                return new QuestionGenerationService.GenerationBatchResult(
                        new com.aimong.backend.domain.mission.service.generation.ModelRoutingPolicy.RoutingDecision("gpt-5-mini", false),
                        List.of(),
                        List.of(rejectedCandidateWithRewriteHint(request))
                );
            }
            return new QuestionGenerationService.GenerationBatchResult(
                    new com.aimong.backend.domain.mission.service.generation.ModelRoutingPolicy.RoutingDecision("gpt-5.4-mini", true),
                    List.of(candidate(request.missionCode(), request.difficultyBand(), request.desiredType(), request.numericDifficulty())),
                    List.of()
            );
        });

        List<QuestionBank> generated = port.generateQuestions(
                missionId,
                new RecompositionSelector.ShortageDetails(
                        1,
                        0,
                        0,
                        0,
                        "INSUFFICIENT_DIFFICULTY_BAND_POOL",
                        new RecompositionSelector.CandidatePoolCounts(9, 4, 3, 2)
                ),
                UUID.randomUUID(),
                false
        );

        assertThat(generated).hasSize(1);

        ArgumentCaptor<QuestionGenerationService.QuestionGenerationRequest> captor =
                ArgumentCaptor.forClass(QuestionGenerationService.QuestionGenerationRequest.class);
        verify(questionGenerationService, org.mockito.Mockito.times(2)).generateValidatedCandidates(captor.capture());
        assertThat(captor.getAllValues().get(1).validationFailureCount()).isEqualTo(1);
        assertThat(captor.getAllValues().get(1).wordingQualityWeak()).isTrue();
        assertThat(captor.getAllValues().get(1).repairHints())
                .contains("Rewrite the stem to avoid repetitive phrasing.");
    }

    private ValidatedDynamicQuestionGenerationPort port(QuestionValidationService validationService) {
        return new ValidatedDynamicQuestionGenerationPort(
                missionRepository,
                questionBankRepository,
                questionGenerationService,
                new GeneratedQuestionPersistenceService(
                        questionBankRepository,
                        questionAnswerKeyRepository,
                        validationService,
                        new ObjectMapper()
                ),
                missionCodeResolver,
                new RuntimeRefillPlanner(),
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 30_000L, 10, 2)
        );
    }

    private QuestionValidationService permissiveValidationService() {
        return new QuestionValidationService(
                new SchemaValidator(),
                new SafetyValidator(),
                new CurriculumFitValidator(new KerisCurriculumRegistry()),
                new StructureRuleValidator(new KerisCurriculumRegistry()),
                new ElementaryReadabilityValidator(),
                new AnswerQualityValidator(),
                new ExplanationQualityValidator(),
                new NaturalnessValidator(),
                new KoreanSurfaceLintValidator(),
                new Step3VocabularyCeilingValidator(),
                new SimilarityDeduplicator(),
                new KerisGoldExampleRegistry(new ObjectMapper()),
                new ObjectMapper()
        ) {
            @Override
            public QuestionValidationReport validate(ValidationRequest request) {
                return new QuestionValidationReport(
                        true,
                        ValidationDecision.SAVE,
                        List.of(),
                        List.of(),
                        new QuestionValidationScores(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
                        null,
                        List.of()
                );
            }
        };
    }

    private QuestionValidationService rejectingValidationService() {
        return new QuestionValidationService(
                new SchemaValidator(),
                new SafetyValidator(),
                new CurriculumFitValidator(new KerisCurriculumRegistry()),
                new StructureRuleValidator(new KerisCurriculumRegistry()),
                new ElementaryReadabilityValidator(),
                new AnswerQualityValidator(),
                new ExplanationQualityValidator(),
                new NaturalnessValidator(),
                new KoreanSurfaceLintValidator(),
                new Step3VocabularyCeilingValidator(),
                new SimilarityDeduplicator(),
                new KerisGoldExampleRegistry(new ObjectMapper()),
                new ObjectMapper()
        ) {
            @Override
            public QuestionValidationReport validate(ValidationRequest request) {
                return new QuestionValidationReport(
                        false,
                        ValidationDecision.RETRY,
                        List.of("forced reject"),
                        List.of(),
                        new QuestionValidationScores(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        null,
                        List.of()
                );
            }
        };
    }

    private StructuredQuestionSchema candidate(
            String missionCode,
            DifficultyBand band,
            QuestionType type,
            int numericDifficulty
    ) {
        return new StructuredQuestionSchema(
                missionCode,
                1,
                band,
                type,
                missionCode + "-" + band + "-" + type + "-" + UUID.randomUUID(),
                optionsFor(type),
                answerFor(type),
                "설명은 두 문장 이하로 유지합니다.",
                List.of("FACT", "SAFETY"),
                "KERIS-REF",
                numericDifficulty
        );
    }

    private QuestionGenerationService.RejectedCandidate rejectedCandidateWithRewriteHint(
            QuestionGenerationService.QuestionGenerationRequest request
    ) {
        return new QuestionGenerationService.RejectedCandidate(
                candidate(request.missionCode(), request.difficultyBand(), request.desiredType(), request.numericDifficulty()),
                new QuestionValidationReport(
                        false,
                        ValidationDecision.REWRITE,
                        List.of("wording drift"),
                        List.of(),
                        new QuestionValidationScores(100, 100, 100, 100, 70, 100, 100, 100, 100, 100, 100, 100, 100, 70, 100, 70, 70),
                        null,
                        List.of("Rewrite the stem to avoid repetitive phrasing.")
                )
        );
    }

    private List<String> optionsFor(QuestionType type) {
        return switch (type) {
            case OX -> null;
            case MULTIPLE -> List.of("A", "B", "C", "D");
            case FILL -> List.of("A", "B", "C", "D");
            case SITUATION -> List.of("A", "B", "C");
        };
    }

    private Object answerFor(QuestionType type) {
        return switch (type) {
            case OX -> Boolean.TRUE;
            case MULTIPLE -> 0;
            case FILL -> List.of(0);
            case SITUATION -> 0;
        };
    }
}
