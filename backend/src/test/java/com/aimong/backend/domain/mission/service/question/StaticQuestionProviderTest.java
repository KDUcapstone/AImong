package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalModePrefersUnusedIntactPack() {
        StaticQuestionProvider provider = new StaticQuestionProvider(
                questionBankRepository,
                quizAttemptRepository,
                new MissionQuestionProperties(10, 30, false),
                objectMapper
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> packQuestions = createQuestions(5, 3, 2);

        when(quizAttemptRepository.findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId))
                .thenReturn(List.of());
        when(questionBankRepository.findIntactPackNumbers(missionId, 10)).thenReturn(List.of((short) 1));
        when(questionBankRepository.findAllByMissionIdAndIsActiveTrueAndPackNoOrderByCreatedAtAsc(missionId, (short) 1))
                .thenReturn(packQuestions);

        List<QuestionBank> selected = provider.findIntactUnusedPack(missionId, childId, false).orElseThrow();

        assertThat(selected).containsExactlyElementsOf(packQuestions);
        verify(questionBankRepository, never()).findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE);
    }

    @Test
    void normalModeExcludesSolvedQuestionsFromCandidatePool() {
        StaticQuestionProvider provider = new StaticQuestionProvider(
                questionBankRepository,
                quizAttemptRepository,
                new MissionQuestionProperties(10, 30, false),
                objectMapper
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> poolQuestions = createQuestions(5, 3, 2);
        UUID solvedId = poolQuestions.getFirst().getId();

        when(quizAttemptRepository.findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId))
                .thenReturn(List.of(createAttempt(childId, missionId, List.of(solvedId))));
        lenient().when(questionBankRepository.findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE))
                .thenReturn(poolQuestions);
        lenient().when(questionBankRepository.findIntactPackNumbers(missionId, 10)).thenReturn(List.of());

        ApprovedQuestionProvider.ApprovedQuestionPool selected = provider.findApprovedQuestionPool(missionId, childId, false);

        assertThat(selected.questions()).extracting(QuestionBank::getId).doesNotContain(solvedId);
        assertThat(selected.excludedBySolved()).isEqualTo(1);
    }

    @Test
    void reviewModeAllowsReuseOfSolvedQuestions() {
        StaticQuestionProvider provider = new StaticQuestionProvider(
                questionBankRepository,
                quizAttemptRepository,
                new MissionQuestionProperties(10, 30, false),
                objectMapper
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> poolQuestions = createQuestions(5, 3, 2);

        when(questionBankRepository.findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE))
                .thenReturn(poolQuestions);

        ApprovedQuestionProvider.ApprovedQuestionPool selected = provider.findApprovedQuestionPool(missionId, childId, true);

        assertThat(selected.questions()).hasSize(10);
        assertThat(selected.excludedBySolved()).isZero();
        verify(quizAttemptRepository, never()).findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId);
    }

    private List<QuestionBank> createQuestions(int lowCount, int mediumCount, int highCount) {
        List<QuestionBank> questions = new java.util.ArrayList<>();
        questions.addAll(createQuestions(lowCount, DifficultyBand.LOW));
        questions.addAll(createQuestions(mediumCount, DifficultyBand.MEDIUM));
        questions.addAll(createQuestions(highCount, DifficultyBand.HIGH));
        return List.copyOf(questions);
    }

    private List<QuestionBank> createQuestions(int count, DifficultyBand band) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);
                    lenient().when(question.getId()).thenReturn(UUID.randomUUID());
                    lenient().when(question.getDifficultyBand()).thenReturn(band);
                    return question;
                })
                .toList();
    }

    private QuizAttempt createAttempt(UUID childId, UUID missionId, List<UUID> questionIds) {
        QuizAttempt attempt = QuizAttempt.create(
                childId,
                missionId,
                writeQuestionIds(questionIds),
                java.time.Instant.now().plusSeconds(300)
        );
        attempt.markSubmitted(java.time.Instant.now());
        return attempt;
    }

    private String writeQuestionIds(List<UUID> questionIds) {
        try {
            return objectMapper.writeValueAsString(questionIds);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
