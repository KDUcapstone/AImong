package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaticQuestionProvider implements ApprovedQuestionProvider {

    private final QuestionBankRepository questionBankRepository;

    @Override
    public List<QuestionBank> findActiveQuestionsBySetIdAndMissionId(String setId, UUID missionId) {
        return copy(questionBankRepository.findAllFromSafeViewBySetIdAndMissionId(setId, missionId));
    }

    @Override
    public List<QuestionBank> findActiveQuestionsByMissionIdAndPackNo(UUID missionId, short packNo) {
        return copy(questionBankRepository.findAllFromSafeViewByMissionIdAndPackNo(missionId, packNo));
    }

    @Override
    public List<QuestionBank> findActiveQuestionsByMissionIdAndDifficulty(UUID missionId, DifficultyBand difficulty) {
        List<QuestionBank> questions = questionBankRepository.findAllFromSafeViewByMissionIdAndDifficulty(
                missionId,
                difficulty.name()
        );
        return copy(questions);
    }

    private List<QuestionBank> copy(List<QuestionBank> questions) {
        return questions == null ? List.of() : List.copyOf(questions);
    }
}
