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
    public List<QuestionBank> findActiveQuestionsByMissionIdAndDifficulty(UUID missionId, DifficultyBand difficulty) {
        List<QuestionBank> questions = questionBankRepository.findAllFromSafeViewByMissionIdAndDifficulty(
                missionId,
                difficulty.name()
        );
        return shuffleIfNeeded(questions);
    }

    private List<QuestionBank> shuffleIfNeeded(List<QuestionBank> questions) {
        if (questions.size() <= 1) {
            return List.copyOf(questions);
        }
        java.util.ArrayList<QuestionBank> shuffled = new java.util.ArrayList<>(questions);
        java.util.Collections.shuffle(shuffled);
        return List.copyOf(shuffled);
    }
}
