package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import java.util.List;
import java.util.UUID;

public interface ApprovedQuestionProvider {

    List<QuestionBank> findActiveQuestionsByMissionIdAndDifficulty(UUID missionId, DifficultyBand difficulty);
}
