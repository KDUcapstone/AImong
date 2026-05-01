package com.aimong.backend.domain.mission.service.question.postmvp;

import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.service.question.RecompositionSelector;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.service.generation.GeneratedQuestionPersistenceService;
import com.aimong.backend.domain.mission.service.generation.MissionCodeResolver;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationRetryFeedback;
import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aimong.mission.question", name = "dynamic-generation-enabled", havingValue = "true")
// Post-MVP runtime generation adapter. MVP question serving never calls this bean.
public class ValidatedDynamicQuestionGenerationPort implements DynamicQuestionGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(ValidatedDynamicQuestionGenerationPort.class);

    private final MissionRepository missionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionGenerationService questionGenerationService;
    private final GeneratedQuestionPersistenceService persistenceService;
    private final MissionCodeResolver missionCodeResolver;
    private final RuntimeRefillPlanner runtimeRefillPlanner;
    private final QuestionGenerationProperties generationProperties;

    @Override
    @Transactional
    public List<QuestionBank> generateQuestions(
            UUID missionId,
            RecompositionSelector.ShortageDetails shortageDetails,
            UUID childId,
            boolean isReview
    ) {
        if (isReview || shortageDetails.totalMissing() <= 0) {
            return List.of();
        }

        Mission mission = missionRepository.findById(missionId).orElse(null);
        if (mission == null) {
            return List.of();
        }

        String missionCode = missionCodeResolver.resolve(mission).orElse(null);
        if (missionCode == null) {
            log.warn("question-generation skipped due to missing missionCode missionId={}", missionId);
            return List.of();
        }

        List<String> existingPrompts = questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId).stream()
                .map(QuestionBank::getPrompt)
                .toList();
        RuntimeRefillPlanner.RuntimeRefillPlan refillPlan = runtimeRefillPlanner.planServingRefill(
                missionCode,
                mission.getStage(),
                shortageDetails,
                existingPrompts
        );

        List<QuestionBank> saved = new ArrayList<>();
        int rejectedCount = 0;
        for (RuntimeRefillPlanner.RuntimeGenerationRequest request : refillPlan.requests()) {
            QuestionGenerationRetryFeedback feedback = QuestionGenerationRetryFeedback.empty();
            int attemptLimit = Math.max(1, generationProperties.miniMaxRetry());
            for (int attempt = 0; attempt < attemptLimit; attempt++) {
                QuestionGenerationService.GenerationBatchResult batchResult =
                        questionGenerationService.generateValidatedCandidates(request.toGenerationRequest(attempt, feedback));
                rejectedCount += batchResult.rejected().size();
                List<QuestionBank> persisted = persistenceService.persistCandidates(
                        missionId,
                        batchResult.accepted(),
                        GenerationPhase.RUNTIME,
                        "GPT"
                );
                saved.addAll(persisted);
                if (!persisted.isEmpty()) {
                    break;
                }
                feedback = feedback.merge(QuestionGenerationRetryFeedback.fromRejected(batchResult.rejected()));
            }
        }

        log.info(
                "dynamic-question-generation missionCode={} missionId={} shortage={} saved={} rejected={} requests={}",
                missionCode,
                missionId,
                shortageDetails.totalMissing(),
                saved.size(),
                rejectedCount,
                refillPlan.requestCount()
        );
        return saved;
    }
}
