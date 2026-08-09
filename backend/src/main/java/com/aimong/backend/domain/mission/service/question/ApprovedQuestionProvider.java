package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import java.util.List;
import java.util.UUID;

public interface ApprovedQuestionProvider {

    default List<QuestionBank> findActiveQuestionsBySetIdAndMissionId(String setId, UUID missionId) {
        return List.of();
    }

    default List<QuestionBank> findActiveQuestionsByMissionIdAndPackNo(UUID missionId, short packNo) {
        return List.of();
    }

    List<QuestionBank> findActiveQuestionsByMissionIdAndDifficulty(UUID missionId, DifficultyBand difficulty);
}
