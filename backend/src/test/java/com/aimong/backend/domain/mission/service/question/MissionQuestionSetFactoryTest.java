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

    @Test
    void selectsStarLevelOneRatio() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();

        stubMissionPools(missionId, 8, 3, 2);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.LOW).count()).isEqualTo(7);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.MEDIUM).count()).isEqualTo(2);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.HIGH).count()).isEqualTo(1);
    }

    @Test
    void selectsStarLevelTwoRatio() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();

        stubMissionPools(missionId, 4, 6, 3);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), 2, false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.LOW).count()).isEqualTo(3);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.MEDIUM).count()).isEqualTo(5);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.HIGH).count()).isEqualTo(2);
    }

    @Test
    void selectsStarLevelThreeRatio() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();

        stubMissionPools(missionId, 3, 4, 6);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), 3, false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.LOW).count()).isEqualTo(2);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.MEDIUM).count()).isEqualTo(3);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.HIGH).count()).isEqualTo(5);
    }

    @Test
    void failsWhenLowPoolIsBelowStarLevelOneQuota() {
        assertMissionSetNotReady(6, 2, 1);
    }

    @Test
    void failsWhenMediumPoolIsBelowStarLevelOneQuota() {
        assertMissionSetNotReady(7, 1, 1);
    }

    @Test
    void failsWhenHighPoolIsBelowStarLevelOneQuota() {
        assertMissionSetNotReady(7, 2, 0);
    }

    @Test
    void failsEvenWhenTotalCountIsTenOrMoreButBandQuotaMisses() {
        assertMissionSetNotReady(10, 0, 10);
    }

    @Test
    void finalSetSizeIsAlwaysTen() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();

        stubMissionPools(missionId, 9, 9, 9);

        assertThat(factory.create(missionId, UUID.randomUUID(), true)).hasSize(10);
    }

    @Test
    void setIdBasedCreateUsesMissionPoolsForApiCompatibility() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        String setId = "S0101-L1";

        stubMissionPools(missionId, 8, 3, 2);

        List<QuestionBank> selected = factory.create(setId, missionId, 1, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.LOW).count()).isEqualTo(7);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.MEDIUM).count()).isEqualTo(2);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.HIGH).count()).isEqualTo(1);
    }

    @Test
    void skipsDuplicatePromptsWithinSelectedQuestionSet() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> lowPool = new ArrayList<>(createQuestions(6, DifficultyBand.LOW, "low"));
        lowPool.add(question(DifficultyBand.LOW, "shared prompt"));
        List<QuestionBank> mediumPool = List.of(
                question(DifficultyBand.MEDIUM, "shared prompt"),
                question(DifficultyBand.MEDIUM, "medium-1"),
                question(DifficultyBand.MEDIUM, "medium-2")
        );
        List<QuestionBank> highPool = createQuestions(1, DifficultyBand.HIGH, "high");
        stubMissionPools(missionId, lowPool, mediumPool, highPool);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().map(QuestionBank::getPrompt).toList()).doesNotHaveDuplicates();
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.LOW).count()).isEqualTo(7);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.MEDIUM).count()).isEqualTo(2);
        assertThat(selected.stream().filter(question -> question.getDifficulty() == DifficultyBand.HIGH).count()).isEqualTo(1);
    }

    @Test
    void failsWhenUniquePromptsCannotFillDifficultyQuota() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> lowPool = new ArrayList<>(createQuestions(6, DifficultyBand.LOW, "low"));
        lowPool.add(question(DifficultyBand.LOW, "shared prompt"));
        List<QuestionBank> mediumPool = List.of(
                question(DifficultyBand.MEDIUM, "shared prompt"),
                question(DifficultyBand.MEDIUM, "medium-1")
        );
        List<QuestionBank> highPool = createQuestions(1, DifficultyBand.HIGH, "high");
        stubMissionPools(missionId, lowPool, mediumPool, highPool);

        assertThatThrownBy(() -> factory.create(missionId, UUID.randomUUID(), false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    private void assertMissionSetNotReady(int lowCount, int mediumCount, int highCount) {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        stubMissionPools(missionId, lowCount, mediumCount, highCount);

        assertThatThrownBy(() -> factory.create(missionId, UUID.randomUUID(), false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    private MissionQuestionSetFactory factory() {
        return new MissionQuestionSetFactory(
                approvedQuestionProvider,
                new MissionQuestionProperties(10, 30, false, false, false, false)
        );
    }

    private void stubMissionPools(UUID missionId, int lowCount, int mediumCount, int highCount) {
        List<QuestionBank> lowPool = createQuestions(lowCount, DifficultyBand.LOW, "low");
        List<QuestionBank> mediumPool = createQuestions(mediumCount, DifficultyBand.MEDIUM, "medium");
        List<QuestionBank> highPool = createQuestions(highCount, DifficultyBand.HIGH, "high");
        stubMissionPools(missionId, lowPool, mediumPool, highPool);
    }

    private void stubMissionPools(
            UUID missionId,
            List<QuestionBank> lowPool,
            List<QuestionBank> mediumPool,
            List<QuestionBank> highPool
    ) {
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.LOW))
                .thenReturn(lowPool);
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.MEDIUM))
                .thenReturn(mediumPool);
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.HIGH))
                .thenReturn(highPool);
    }

    private List<QuestionBank> createQuestions(int count, DifficultyBand difficulty, String promptPrefix) {
        List<QuestionBank> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            questions.add(question(difficulty, promptPrefix + "-" + index));
        }
        return List.copyOf(questions);
    }

    private QuestionBank question(DifficultyBand difficulty, String prompt) {
        QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);
        when(question.getId()).thenReturn(UUID.randomUUID());
        when(question.getDifficulty()).thenReturn(difficulty);
        when(question.getPrompt()).thenReturn(prompt);
        return question;
    }
}
