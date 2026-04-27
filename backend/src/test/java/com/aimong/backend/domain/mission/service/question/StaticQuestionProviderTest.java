package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
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
        MissionQuestionProperties properties = new MissionQuestionProperties(10, 30, false);
        StaticQuestionProvider provider = new StaticQuestionProvider(
                questionBankRepository,
                quizAttemptRepository,
                properties,
                objectMapper
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> packQuestions = createQuestions(10);

        when(quizAttemptRepository.findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId))
                .thenReturn(List.of());
        when(questionBankRepository.findIntactPackNumbers(missionId, 10))
                .thenReturn(List.of((short) 1));
        when(questionBankRepository.findAllByMissionIdAndIsActiveTrueAndPackNoOrderByCreatedAtAsc(missionId, (short) 1))
                .thenReturn(packQuestions);

        List<QuestionBank> selected = provider.findApprovedQuestions(missionId, childId, false);

        assertThat(selected).containsExactlyElementsOf(packQuestions);
        verify(questionBankRepository, never()).findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE);
    }

    @Test
    void normalModeExcludesSolvedQuestionsWhenRecomposingPool() {
        MissionQuestionProperties properties = new MissionQuestionProperties(10, 30, false);
        StaticQuestionProvider provider = new StaticQuestionProvider(
                questionBankRepository,
                quizAttemptRepository,
                properties,
                objectMapper
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> poolQuestions = createQuestions(4);
        for (QuestionBank question : poolQuestions) {
            when(question.getId()).thenReturn(UUID.randomUUID());
        }
        UUID solvedId = poolQuestions.get(0).getId();

        when(quizAttemptRepository.findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId))
                .thenReturn(List.of(createAttempt(childId, missionId, List.of(solvedId))));
        lenient().when(questionBankRepository.findIntactPackNumbers(missionId, 10))
                .thenReturn(List.of());
        lenient().when(questionBankRepository.findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE))
                .thenReturn(poolQuestions);

        List<QuestionBank> selected = provider.findApprovedQuestions(missionId, childId, false);

        assertThat(selected).extracting(QuestionBank::getId).doesNotContain(solvedId);
    }

    @Test
    void reviewModeAllowsReuseOfSolvedQuestions() {
        MissionQuestionProperties properties = new MissionQuestionProperties(10, 30, false);
        StaticQuestionProvider provider = new StaticQuestionProvider(
                questionBankRepository,
                quizAttemptRepository,
                properties,
                objectMapper
        );

        UUID missionId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        List<QuestionBank> poolQuestions = createQuestions(3);

        when(questionBankRepository.findIntactPackNumbers(missionId, 10))
                .thenReturn(List.of());
        when(questionBankRepository.findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE))
                .thenReturn(poolQuestions);

        List<QuestionBank> selected = provider.findApprovedQuestions(missionId, childId, true);

        assertThat(selected).hasSize(3);
        verify(quizAttemptRepository, never()).findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId);
    }

    private List<QuestionBank> createQuestions(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    return org.mockito.Mockito.mock(QuestionBank.class);
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
