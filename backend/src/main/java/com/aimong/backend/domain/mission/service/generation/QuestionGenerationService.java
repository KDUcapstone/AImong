package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final KerisCurriculumRegistry kerisCurriculumRegistry;
    private final ModelRoutingPolicy modelRoutingPolicy;
    private final QuestionCandidateGenerator questionCandidateGenerator;
    private final QuestionValidator questionValidator;
    private final SimilarityDeduplicator similarityDeduplicator;

    public GenerationBatchResult generateValidatedCandidates(QuestionGenerationRequest request) {
        KerisCurriculumRegistry.KerisMissionRule missionRule = kerisCurriculumRegistry.findMissionRule(request.missionCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown missionCode: " + request.missionCode()));

        ModelRoutingPolicy.RoutingDecision routingDecision = modelRoutingPolicy.decide(
                new ModelRoutingPolicy.GenerationContext(
                        missionRule.stage(),
                        request.difficultyBand(),
                        request.numericDifficulty(),
                        request.validationFailureCount(),
                        request.wordingQualityWeak(),
                        request.highDuplicateRisk(),
                        request.optionQualityWeak(),
                        request.explanationQualityWeak()
                )
        );

        List<StructuredQuestionSchema> rawCandidates = questionCandidateGenerator.generate(request, routingDecision.selectedModel());
        List<StructuredQuestionSchema> accepted = new ArrayList<>();
        List<RejectedCandidate> rejected = new ArrayList<>();
        List<String> knownTexts = new ArrayList<>(request.existingMissionPrompts());
        knownTexts.addAll(request.goldExamplePrompts());

        for (StructuredQuestionSchema candidate : rawCandidates) {
            List<String> errors = new ArrayList<>(questionValidator.validate(candidate));
            errors.addAll(similarityDeduplicator.validate(candidate.question(), knownTexts));
            if (errors.isEmpty()) {
                accepted.add(candidate);
                knownTexts.add(candidate.question());
            } else {
                rejected.add(new RejectedCandidate(candidate, errors));
            }
        }

        log.info(
                "question-generation evaluated missionCode={} packNo={} difficultyBand={} selectedModel={} accepted={} rejected={}",
                request.missionCode(),
                request.packNo(),
                request.difficultyBand(),
                routingDecision.selectedModel(),
                accepted.size(),
                rejected.size()
        );

        return new GenerationBatchResult(routingDecision, accepted, rejected);
    }

    public record QuestionGenerationRequest(
            String missionCode,
            int packNo,
            DifficultyBand difficultyBand,
            QuestionType desiredType,
            int candidateCount,
            int numericDifficulty,
            int validationFailureCount,
            boolean wordingQualityWeak,
            boolean highDuplicateRisk,
            boolean optionQualityWeak,
            boolean explanationQualityWeak,
            List<String> existingMissionPrompts,
            List<String> goldExamplePrompts
    ) {
    }

    public record GenerationBatchResult(
            ModelRoutingPolicy.RoutingDecision routingDecision,
            List<StructuredQuestionSchema> accepted,
            List<RejectedCandidate> rejected
    ) {
    }

    public record RejectedCandidate(
            StructuredQuestionSchema candidate,
            List<String> reasons
    ) {
    }
}
