package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
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
    private final MissionQuestionProperties missionQuestionProperties;
    private final RecompositionSelector recompositionSelector;

    public List<QuestionBank> create(UUID missionId, UUID childId, boolean isReview) {
        List<QuestionBank> lowPool = approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.LOW);
        List<QuestionBank> mediumPool = approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.MEDIUM);
        List<QuestionBank> highPool = approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.HIGH);

        RecompositionSelector.SelectionResult selection = recompositionSelector.select(lowPool, mediumPool, highPool);
        if (selection instanceof RecompositionSelector.Composed composed) {
            return shuffleFinalSet(composed.questionSet());
        }
        throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
    }

    private List<QuestionBank> shuffleFinalSet(List<QuestionBank> questionSet) {
        List<QuestionBank> shuffled = new java.util.ArrayList<>(questionSet);
        Collections.shuffle(shuffled);
        if (shuffled.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        return List.copyOf(shuffled);
    }
}
