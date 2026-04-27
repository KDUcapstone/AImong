package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MissionQuestionSetFactory {

    private final ApprovedQuestionProvider approvedQuestionProvider;
    private final DynamicQuestionGenerationPort dynamicQuestionGenerationPort;
    private final MissionQuestionProperties missionQuestionProperties;
    private final QuestionGenerationProperties generationProperties;
    private final RecompositionSelector recompositionSelector;

    public List<QuestionBank> create(UUID missionId, UUID childId, boolean isReview) {
        List<QuestionBank> intactPack = approvedQuestionProvider.findIntactUnusedPack(missionId, childId, isReview)
                .orElse(List.of());
        if (intactPack.size() == missionQuestionProperties.setSize()) {
            return List.copyOf(intactPack);
        }

        ApprovedQuestionProvider.ApprovedQuestionPool approvedPool =
                approvedQuestionProvider.findApprovedQuestionPool(missionId, childId, isReview);
        RecompositionSelector.SelectionResult initialSelection = recompositionSelector.select(approvedPool);
        if (initialSelection instanceof RecompositionSelector.Composed composed) {
            return shuffleFinalSet(composed.questionSet());
        }

        if (!missionQuestionProperties.dynamicGenerationEnabled()
                || !(initialSelection instanceof RecompositionSelector.Shortage shortage)) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }

        RecompositionSelector.Shortage currentShortage = shortage;
        int attempts = Math.max(1, generationProperties.miniMaxRetry());
        for (int attempt = 0; attempt < attempts; attempt++) {
            dynamicQuestionGenerationPort.generateQuestions(
                    missionId,
                    limitToSyncBatch(currentShortage.details()),
                    childId,
                    isReview
            );

            ApprovedQuestionProvider.ApprovedQuestionPool replenishedPool =
                    approvedQuestionProvider.findApprovedQuestionPool(missionId, childId, isReview);
            RecompositionSelector.SelectionResult replenishedSelection = recompositionSelector.select(replenishedPool);
            if (replenishedSelection instanceof RecompositionSelector.Composed composed) {
                return shuffleFinalSet(composed.questionSet());
            }
            if (!(replenishedSelection instanceof RecompositionSelector.Shortage nextShortage)) {
                break;
            }
            currentShortage = nextShortage;
        }
        throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
    }

    private RecompositionSelector.ShortageDetails limitToSyncBatch(RecompositionSelector.ShortageDetails details) {
        int remainingBudget = Math.max(1, generationProperties.syncGenerateBatch());
        int lowMissing = Math.min(details.lowMissing(), remainingBudget);
        remainingBudget -= lowMissing;
        int mediumMissing = Math.min(details.mediumMissing(), remainingBudget);
        remainingBudget -= mediumMissing;
        int highMissing = Math.min(details.highMissing(), remainingBudget);
        return new RecompositionSelector.ShortageDetails(
                lowMissing,
                mediumMissing,
                highMissing,
                details.excludedBySolved(),
                details.reason(),
                details.candidatePoolCounts()
        );
    }

    private List<QuestionBank> shuffleFinalSet(List<QuestionBank> questionSet) {
        List<QuestionBank> shuffled = new java.util.ArrayList<>(questionSet);
        Collections.shuffle(shuffled);
        return List.copyOf(shuffled);
    }
}
