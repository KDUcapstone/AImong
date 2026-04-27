package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.ArrayList;
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

    public List<QuestionBank> create(UUID missionId, UUID childId, boolean isReview) {
        List<QuestionBank> pool = new ArrayList<>(
                approvedQuestionProvider.findApprovedQuestions(missionId, childId, isReview)
        );

        int requiredCount = missionQuestionProperties.setSize();
        if (pool.size() == requiredCount) {
            return List.copyOf(pool);
        }

        int shortage = requiredCount - pool.size();
        if (shortage > 0 && missionQuestionProperties.dynamicGenerationEnabled()) {
            pool.addAll(dynamicQuestionGenerationPort.generateQuestions(missionId, shortage, childId, isReview));
        }

        if (pool.size() < requiredCount) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }

        Collections.shuffle(pool);
        return List.copyOf(pool.subList(0, requiredCount));
    }
}
