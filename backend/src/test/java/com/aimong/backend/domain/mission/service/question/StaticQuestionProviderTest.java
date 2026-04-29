package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaticQuestionProviderTest {

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Test
    void readsOnlyActiveQuestionsForRequestedDifficulty() {
        StaticQuestionProvider provider = new StaticQuestionProvider(questionBankRepository);
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> questions = List.of(question(DifficultyBand.LOW), question(DifficultyBand.LOW));

        when(questionBankRepository.findAllFromSafeViewByMissionIdAndDifficulty(missionId, DifficultyBand.LOW.name()))
                .thenReturn(questions);

        List<QuestionBank> selected = provider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.LOW);

        assertThat(selected).hasSize(2);
        verify(questionBankRepository).findAllFromSafeViewByMissionIdAndDifficulty(missionId, DifficultyBand.LOW.name());
    }

    private QuestionBank question(DifficultyBand difficulty) {
        return org.mockito.Mockito.mock(QuestionBank.class);
    }
}
