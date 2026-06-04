package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Comparator;
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
    void starLevelOneUsesFixedLowSet() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> lowPool = createQuestions(12, DifficultyBand.LOW, "low");
        stubDifficultyPool(missionId, DifficultyBand.LOW, lowPool);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), 1, false);

        assertThat(selected).hasSize(10);
        assertThat(selected).allMatch(question -> question.getDifficulty() == DifficultyBand.LOW);
        assertThat(ids(selected)).containsExactlyInAnyOrderElementsOf(ids(lowPool.subList(0, 10)));
    }

    @Test
    void starLevelTwoUsesFixedMediumSet() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> mediumPool = createQuestions(12, DifficultyBand.MEDIUM, "medium");
        stubDifficultyPool(missionId, DifficultyBand.MEDIUM, mediumPool);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), 2, false);

        assertThat(selected).hasSize(10);
        assertThat(selected).allMatch(question -> question.getDifficulty() == DifficultyBand.MEDIUM);
        assertThat(ids(selected)).containsExactlyInAnyOrderElementsOf(ids(mediumPool.subList(0, 10)));
    }

    @Test
    void starLevelThreeUsesFixedHighSet() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> highPool = createQuestions(12, DifficultyBand.HIGH, "high");
        stubDifficultyPool(missionId, DifficultyBand.HIGH, highPool);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), 3, false);

        assertThat(selected).hasSize(10);
        assertThat(selected).allMatch(question -> question.getDifficulty() == DifficultyBand.HIGH);
        assertThat(ids(selected)).containsExactlyInAnyOrderElementsOf(ids(highPool.subList(0, 10)));
    }

    @Test
    void setIdBasedCreateUsesExactFixedSetBeforeDifficultyFallback() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        String setId = "S0101-L2";
        List<QuestionBank> setPool = new ArrayList<>();
        setPool.addAll(createQuestions(4, DifficultyBand.LOW, "low"));
        setPool.addAll(createQuestions(3, DifficultyBand.MEDIUM, "medium"));
        setPool.addAll(createQuestions(3, DifficultyBand.HIGH, "high"));
        when(approvedQuestionProvider.findActiveQuestionsBySetIdAndMissionId(setId, missionId))
                .thenReturn(setPool);

        List<QuestionBank> selected = factory.create(setId, missionId, 2, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(ids(selected)).containsExactlyInAnyOrderElementsOf(ids(setPool));
        assertThat(difficultyRanks(selected)).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void setIdBasedCreateSkipsDuplicatePromptsAndUsesStableExtraCandidate() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        String setId = "S0101-L1";
        List<QuestionBank> setPool = new ArrayList<>();
        QuestionBank firstShared = question(DifficultyBand.LOW, "shared prompt");
        QuestionBank duplicateShared = question(DifficultyBand.LOW, "shared prompt");
        QuestionBank stableExtra = question(DifficultyBand.LOW, "stable extra");
        setPool.add(firstShared);
        setPool.add(duplicateShared);
        setPool.addAll(createQuestions(8, DifficultyBand.LOW, "low"));
        setPool.add(stableExtra);
        when(approvedQuestionProvider.findActiveQuestionsBySetIdAndMissionId(setId, missionId))
                .thenReturn(setPool);

        List<QuestionBank> selected = factory.create(setId, missionId, 1, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(selected).extracting(QuestionBank::getPrompt).doesNotHaveDuplicates();
        assertThat(ids(selected)).contains(firstShared.getId(), stableExtra.getId());
        assertThat(ids(selected)).doesNotContain(duplicateShared.getId());
    }

    @Test
    void setIdFallbackUsesMatchingPackNoWhenSetIdRowsAreAbsent() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        String setId = "S0101-L2";
        List<QuestionBank> packPool = createQuestions(10, DifficultyBand.MEDIUM, "pack-medium");
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndPackNo(missionId, (short) 2))
                .thenReturn(packPool);

        List<QuestionBank> selected = factory.create(setId, missionId, 2, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(ids(selected)).containsExactlyInAnyOrderElementsOf(ids(packPool));
    }

    @Test
    void setIdFallbackIgnoresPackNoThatDoesNotMatchStarLevel() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        String setId = "S0101-L3";
        List<QuestionBank> mediumPool = createQuestions(10, DifficultyBand.MEDIUM, "medium");
        stubDifficultyPool(missionId, DifficultyBand.MEDIUM, mediumPool);

        List<QuestionBank> selected = factory.create(setId, missionId, 2, UUID.randomUUID(), false);

        assertThat(selected).hasSize(10);
        assertThat(ids(selected)).containsExactlyInAnyOrderElementsOf(ids(mediumPool));
        verify(approvedQuestionProvider, never()).findActiveQuestionsByMissionIdAndPackNo(missionId, (short) 3);
    }

    @Test
    void failsWhenTargetDifficultyPoolIsBelowSetSize() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        stubDifficultyPool(missionId, DifficultyBand.LOW, createQuestions(9, DifficultyBand.LOW, "low"));

        assertThatThrownBy(() -> factory.create(missionId, UUID.randomUUID(), 1, false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    @Test
    void failsEvenWhenOtherBandsHaveEnoughButTargetBandIsShort() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        stubDifficultyPool(missionId, DifficultyBand.MEDIUM, createQuestions(9, DifficultyBand.MEDIUM, "medium"));
        stubDifficultyPool(missionId, DifficultyBand.LOW, createQuestions(20, DifficultyBand.LOW, "low"));
        stubDifficultyPool(missionId, DifficultyBand.HIGH, createQuestions(20, DifficultyBand.HIGH, "high"));

        assertThatThrownBy(() -> factory.create(missionId, UUID.randomUUID(), 2, false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    @Test
    void failsWhenUniquePromptsCannotFillFixedSet() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> lowPool = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            lowPool.add(question(DifficultyBand.LOW, "shared prompt"));
        }
        stubDifficultyPool(missionId, DifficultyBand.LOW, lowPool);

        assertThatThrownBy(() -> factory.create(missionId, UUID.randomUUID(), 1, false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    @Test
    void skipsNearDuplicatePromptsWithinFixedSet() {
        MissionQuestionSetFactory factory = factory();
        UUID missionId = UUID.randomUUID();
        List<QuestionBank> lowPool = new ArrayList<>();
        lowPool.add(question(DifficultyBand.LOW, "\uBE44\uBC00\uBC88\uD638\uB97C \uCE5C\uAD6C\uC5D0\uAC8C \uC54C\uB824 \uC8FC\uBA74 \uC65C \uC704\uD5D8\uD55C\uAC00\uC694?"));
        lowPool.add(question(DifficultyBand.LOW, "\uBE44\uBC00\uBC88\uD638\uB294 \uCE5C\uAD6C\uD55C\uD14C \uC54C\uB824\uC8FC\uBA74 \uC65C \uC704\uD5D8\uD560\uAE4C\uC694?"));
        lowPool.addAll(createQuestions(9, DifficultyBand.LOW, "low"));
        stubDifficultyPool(missionId, DifficultyBand.LOW, lowPool);

        List<QuestionBank> selected = factory.create(missionId, UUID.randomUUID(), 1, false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream()
                .filter(question -> question.getPrompt().contains("\uBE44\uBC00\uBC88\uD638"))
                .map(QuestionBank::getPrompt)
                .toList()).hasSize(1);
    }

    @Test
    void invalidStarLevelFails() {
        MissionQuestionSetFactory factory = factory();

        assertThatThrownBy(() -> factory.create(UUID.randomUUID(), UUID.randomUUID(), 4, false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STAR_LEVEL);
    }

    private MissionQuestionSetFactory factory() {
        return new MissionQuestionSetFactory(
                approvedQuestionProvider,
                new MissionQuestionProperties(10, 30, false)
        );
    }

    private void stubDifficultyPool(UUID missionId, DifficultyBand difficulty, List<QuestionBank> questions) {
        when(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, difficulty))
                .thenReturn(questions);
    }

    private List<QuestionBank> createQuestions(int count, DifficultyBand difficulty, String promptPrefix) {
        List<QuestionBank> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            questions.add(question(difficulty, promptPrefix + " topic" + index));
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

    private List<UUID> ids(List<QuestionBank> questions) {
        return questions.stream()
                .map(QuestionBank::getId)
                .toList();
    }

    private List<Integer> difficultyRanks(List<QuestionBank> questions) {
        return questions.stream()
                .map(question -> switch (question.getDifficulty()) {
                    case LOW -> 1;
                    case MEDIUM -> 2;
                    case HIGH -> 3;
                })
                .toList();
    }
}
