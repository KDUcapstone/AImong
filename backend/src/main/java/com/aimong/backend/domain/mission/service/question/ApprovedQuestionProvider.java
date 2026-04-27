package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovedQuestionProvider {

    Optional<List<QuestionBank>> findIntactUnusedPack(UUID missionId, UUID childId, boolean isReview);

    ApprovedQuestionPool findApprovedQuestionPool(UUID missionId, UUID childId, boolean isReview);

    record ApprovedQuestionPool(
            List<QuestionBank> questions,
            int excludedBySolved
    ) {
    }
}
