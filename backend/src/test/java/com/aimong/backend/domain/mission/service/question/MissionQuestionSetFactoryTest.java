package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MissionQuestionSetFactoryTest {

    @Mock
    private ApprovedQuestionProvider approvedQuestionProvider;

    private final RecompositionSelector recompositionSelector = new RecompositionSelector();

    @Test
    void selectsExactlyLowFiveMediumThreeHighTwo() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();

        stubPools(missionId, 6, 4, 3);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.LOW).count()).isEqualTo(5);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.MEDIUM).count()).isEqualTo(3);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.HIGH).count()).isEqualTo(2);
    }

    @Test
    void failsWhenLowPoolIsBelowFive() {
        assertMissionSetNotReady(4, 3, 2);
    }

    @Test
    void failsWhenMediumPoolIsBelowThree() {
        assertMissionSetNotReady(5, 2, 2);
    }

    @Test
    void failsWhenHighPoolIsBelowTwo() {
        assertMissionSetNotReady(5, 3, 1);
    }

    @Test
    void failsEvenWhenTotalCountIsTenOrMoreButBandQuotaMisses() {
        assertMissionSetNotReady(10, 0, 10);
    }

    @Test
    void finalSetSizeIsAlwaysTen() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();

        stubPools(missionId, 9, 9, 9);

        assertThat(factory.create(missionId, UUID.randomUUID(), true)).hasSize(10);
    }

    private void assertMissionSetNotReady(int lowCount, int mediumCount, int highCount) {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        stubPools(missionId, lowCount, mediumCount, highCount);

        assertThatThrownBy(() -> factory.create(missionId, UUID.randomUUID(), false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    private MissionQuestionSetFactory factory() {
        return new MissionQuestionSetFactory(
                approvedQuestionProvider,
                new MissionQuestionProperties(10, 30, false, false, false, false),
                recompositionSelector
        );
    }

    private void stubPools(UUID missionId, int lowCount, int mediumCount, int highCount) {
        List<QuestionBank> lowPool = createQuestions(lowCount, DifficultyBand.LOW);
        List<QuestionBank> mediumPool = createQuestions(mediumCount, DifficultyBand.MEDIUM);
        List<QuestionBank> highPool = createQuestions(highCount, DifficultyBand.HIGH);
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.LOW))
                .thenReturn(lowPool);
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.MEDIUM))
                .thenReturn(mediumPool);
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.HIGH))
                .thenReturn(highPool);
    }

    private List<QuestionBank> createQuestions(int count, DifficultyBand difficulty) {
        List<QuestionBank> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);
            when(question.getDifficulty()).thenReturn(difficulty);
            questions.add(question);
        }
        return List.copyOf(questions);
    }
}
