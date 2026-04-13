package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaticQuestionProvider implements ApprovedQuestionProvider {

    private final QuestionBankRepository questionBankRepository;

    @Override
    public List<QuestionBank> findApprovedQuestions(UUID missionId, UUID childId, boolean isReview) {
        List<QuestionBank> questions = new ArrayList<>(questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId));
        Collections.shuffle(questions);
        return questions;
    }
}
