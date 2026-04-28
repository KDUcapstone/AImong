package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MissionQuestionSetFactoryTest {

    @Mock
    private ApprovedQuestionProvider approvedQuestionProvider;

    @Mock
    private DynamicQuestionGenerationPort dynamicQuestionGenerationPort;

    private final RecompositionSelector recompositionSelector = new RecompositionSelector();

    @Test
    void returnsExactIntactPackBeforeRecomposition() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true),
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 2_000L, 10, 2),
                recompositionSelector
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> pack = createQuestions(5, 3, 2);

        when(approvedQuestionProvider.findIntactUnusedPack(missionId, childId, false)).thenReturn(Optional.of(pack));

        List<QuestionBank> selected = factory.create(missionId, childId, false);

        assertThat(selected).containsExactlyInAnyOrderElementsOf(pack);
        verify(approvedQuestionProvider, never()).findApprovedQuestionPool(any(), any(), any(Boolean.class));
        verify(dynamicQuestionGenerationPort, never()).generateQuestions(any(), any(), any(), any(Boolean.class));
    }

    @Test
    void recomposesToLowFiveMediumThreeHighTwoWhenNoIntactPack() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, false),
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 2_000L, 10, 2),
                recompositionSelector
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        ApprovedQuestionProvider.ApprovedQuestionPool pool =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(7, 4, 3), 0);

        when(approvedQuestionProvider.findIntactUnusedPack(missionId, childId, false)).thenReturn(Optional.empty());
        when(approvedQuestionProvider.findApprovedQuestionPool(missionId, childId, false)).thenReturn(pool);

        List<QuestionBank> selected = factory.create(missionId, childId, false);

        assertThat(selected).hasSize(10);
        assertThat(selected.stream().filter(question -> question.getDifficultyBand() == DifficultyBand.LOW).count()).isEqualTo(5);
        assertThat(selected.stream().filter(question -> question.getDifficultyBand() == DifficultyBand.MEDIUM).count()).isEqualTo(3);
        assertThat(selected.stream().filter(question -> question.getDifficultyBand() == DifficultyBand.HIGH).count()).isEqualTo(2);
    }

    @Test
    void retriesRecompositionAfterRuntimeRefillWhenSelectorReturnsShortage() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true),
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 2_000L, 10, 2),
                recompositionSelector
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        ApprovedQuestionProvider.ApprovedQuestionPool initialPool =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 3, 1), 2);
        ApprovedQuestionProvider.ApprovedQuestionPool replenishedPool =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 3, 2), 2);

        when(approvedQuestionProvider.findIntactUnusedPack(missionId, childId, false)).thenReturn(Optional.empty());
        when(approvedQuestionProvider.findApprovedQuestionPool(missionId, childId, false))
                .thenReturn(initialPool)
                .thenReturn(replenishedPool);
        when(dynamicQuestionGenerationPort.generateQuestions(eq(missionId), any(RecompositionSelector.ShortageDetails.class), eq(childId), eq(false)))
                .thenReturn(List.of());

        List<QuestionBank> selected = factory.create(missionId, childId, false);

        assertThat(selected).hasSize(10);
        verify(dynamicQuestionGenerationPort).generateQuestions(
                eq(missionId),
                any(RecompositionSelector.ShortageDetails.class),
                eq(childId),
                eq(false)
        );
    }

    @Test
    void throwsMissionSetNotReadyWhenBandStillMissingAfterRefillRetry() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true),
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 2_000L, 10, 2),
                recompositionSelector
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        ApprovedQuestionProvider.ApprovedQuestionPool shortagePool =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 2, 2), 1);

        when(approvedQuestionProvider.findIntactUnusedPack(missionId, childId, false)).thenReturn(Optional.empty());
        when(approvedQuestionProvider.findApprovedQuestionPool(missionId, childId, false))
                .thenReturn(shortagePool)
                .thenReturn(shortagePool);
        when(dynamicQuestionGenerationPort.generateQuestions(eq(missionId), any(RecompositionSelector.ShortageDetails.class), eq(childId), eq(false)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> factory.create(missionId, childId, false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);
    }

    @Test
    void retriesSyncRefillUpToConfiguredMiniMaxRetry() {
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                approvedQuestionProvider,
                dynamicQuestionGenerationPort,
                new MissionQuestionProperties(10, 30, true),
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 2_000L, 10, 3),
                recompositionSelector
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        ApprovedQuestionProvider.ApprovedQuestionPool firstShortage =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 3, 1), 2);
        ApprovedQuestionProvider.ApprovedQuestionPool secondShortage =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 3, 1), 1);
        ApprovedQuestionProvider.ApprovedQuestionPool replenishedPool =
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 3, 2), 0);

        when(approvedQuestionProvider.findIntactUnusedPack(missionId, childId, false)).thenReturn(Optional.empty());
        when(approvedQuestionProvider.findApprovedQuestionPool(missionId, childId, false))
                .thenReturn(firstShortage)
                .thenReturn(secondShortage)
                .thenReturn(replenishedPool);
        when(dynamicQuestionGenerationPort.generateQuestions(eq(missionId), any(RecompositionSelector.ShortageDetails.class), eq(childId), eq(false)))
                .thenReturn(List.of());

        List<QuestionBank> selected = factory.create(missionId, childId, false);

        assertThat(selected).hasSize(10);
        verify(dynamicQuestionGenerationPort, org.mockito.Mockito.times(2)).generateQuestions(
                eq(missionId),
                any(RecompositionSelector.ShortageDetails.class),
                eq(childId),
                eq(false)
        );
    }

    @Test
    void reviewModeAllowsSolvedReuseBecausePoolAlreadyContainsReusableQuestions() {
        RecompositionSelector.SelectionResult selection = recompositionSelector.select(
                new ApprovedQuestionProvider.ApprovedQuestionPool(createQuestions(5, 3, 2), 0)
        );

        assertThat(selection).isInstanceOf(RecompositionSelector.Composed.class);
    }

    private List<QuestionBank> createQuestions(int lowCount, int mediumCount, int highCount) {
        List<QuestionBank> questions = new ArrayList<>();
        questions.addAll(createQuestions(lowCount, DifficultyBand.LOW));
        questions.addAll(createQuestions(mediumCount, DifficultyBand.MEDIUM));
        questions.addAll(createQuestions(highCount, DifficultyBand.HIGH));
        return List.copyOf(questions);
    }

    private List<QuestionBank> createQuestions(int count, DifficultyBand band) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);
                    lenient().when(question.getDifficultyBand()).thenReturn(band);
                    return question;
                })
                .toList();
    }
}
