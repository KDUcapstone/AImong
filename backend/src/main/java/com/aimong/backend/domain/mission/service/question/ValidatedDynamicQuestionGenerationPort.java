package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.service.generation.GeneratedQuestionPersistenceService;
import com.aimong.backend.domain.mission.service.generation.KerisCurriculumRegistry;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import com.aimong.backend.domain.mission.service.generation.StructuredQuestionSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ValidatedDynamicQuestionGenerationPort implements DynamicQuestionGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(ValidatedDynamicQuestionGenerationPort.class);

    private final MissionRepository missionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionGenerationService questionGenerationService;
    private final GeneratedQuestionPersistenceService persistenceService;
    private final KerisCurriculumRegistry kerisCurriculumRegistry;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<QuestionBank> generateQuestions(UUID missionId, int shortage, UUID childId, boolean isReview) {
        if (isReview || shortage <= 0) {
            return List.of();
        }

        Mission mission = missionRepository.findById(missionId).orElse(null);
        if (mission == null) {
            return List.of();
        }

        String missionCode = resolveMissionCode(mission);
        if (missionCode == null) {
            log.warn("question-generation skipped due to missing missionCode missionId={}", missionId);
            return List.of();
        }

        QuestionGenerationService.GenerationBatchResult batchResult = questionGenerationService.generateValidatedCandidates(
                new QuestionGenerationService.QuestionGenerationRequest(
                        missionCode,
                        1,
                        inferDifficultyBand(mission.getStage()),
                        com.aimong.backend.domain.mission.entity.QuestionType.MULTIPLE,
                        shortage,
                        inferDifficulty(mission.getStage()),
                        0,
                        false,
                        false,
                        false,
                        false,
                        questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId).stream()
                                .map(QuestionBank::getPrompt)
                                .toList(),
                        List.of()
                )
        );

        List<QuestionBank> saved = persistenceService.persistCandidates(
                missionId,
                batchResult.accepted().stream().limit(shortage).toList(),
                GenerationPhase.RUNTIME,
                "GPT"
        );

        log.info(
                "dynamic-question-generation missionCode={} missionId={} shortage={} saved={} rejected={}",
                missionCode,
                missionId,
                shortage,
                saved.size(),
                batchResult.rejected().size()
        );
        return saved;
    }

    private String resolveMissionCode(Mission mission) {
        if (mission.getMissionCode() != null && !mission.getMissionCode().isBlank()) {
            return mission.getMissionCode();
        }
        return kerisCurriculumRegistry.findMissionRuleByStageAndTitle(mission.getStage(), mission.getTitle())
                .map(KerisCurriculumRegistry.KerisMissionRule::missionCode)
                .orElse(null);
    }

    private com.aimong.backend.domain.mission.entity.DifficultyBand inferDifficultyBand(short stage) {
        return switch (stage) {
            case 1 -> com.aimong.backend.domain.mission.entity.DifficultyBand.LOW;
            case 2 -> com.aimong.backend.domain.mission.entity.DifficultyBand.MEDIUM;
            case 3 -> com.aimong.backend.domain.mission.entity.DifficultyBand.HIGH;
            default -> com.aimong.backend.domain.mission.entity.DifficultyBand.LOW;
        };
    }

    private int inferDifficulty(short stage) {
        return switch (stage) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 1;
        };
    }

}
