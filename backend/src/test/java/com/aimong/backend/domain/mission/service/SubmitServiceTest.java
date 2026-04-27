package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ProfileImageType;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import com.aimong.backend.domain.mission.entity.MissionAttempt;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionAnswerResultRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.pet.service.PetGrowthService;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.MilestoneRewardRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitServiceTest {

    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private QuestionBankRepository questionBankRepository;
    @Mock private QuestionAnswerKeyRepository questionAnswerKeyRepository;
    @Mock private MissionAnswerResultRepository missionAnswerResultRepository;
    @Mock private MissionAttemptRepository missionAttemptRepository;
    @Mock private MissionDailyProgressRepository missionDailyProgressRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;
    @Mock private TicketRepository ticketRepository;
    @Mock private StreakRecordRepository streakRecordRepository;
    @Mock private FriendStreakRepository friendStreakRepository;
    @Mock private MilestoneRewardRepository milestoneRewardRepository;
    @Mock private DailyQuestService dailyQuestService;
    @Mock private WeeklyQuestService weeklyQuestService;
    @Mock private AchievementService achievementService;
    @Mock private PetGrowthService petGrowthService;
    @Mock private QuizService quizService;
    @Mock private MissionService missionService;

    @Test
    void failureSubmissionPersistsMissionAttemptAndAnswerResults() {
        SubmitService service = service();
        Fixture fixture = fixture(false);

        SubmitResponse response = service.submit(fixture.childId(), fixture.missionId(), fixture.request());

        assertThat(response.isPassed()).isFalse();
        assertThat(response.progressApplied()).isFalse();

        ArgumentCaptor<MissionAttempt> attemptCaptor = ArgumentCaptor.forClass(MissionAttempt.class);
        verify(missionAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().isReview()).isFalse();
        assertThat(attemptCaptor.getValue().isPassed()).isFalse();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MissionAnswerResult>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(missionAnswerResultRepository).saveAll(resultsCaptor.capture());
        assertThat(resultsCaptor.getValue()).hasSize(10);
        assertThat(resultsCaptor.getValue()).allSatisfy(result -> {
            assertThat(result.isReview()).isFalse();
            assertThat(result.isCorrect()).isFalse();
        });
    }

    @Test
    void reviewSubmissionPersistsReviewAttemptAndAnswerResults() {
        SubmitService service = service();
        Fixture fixture = fixture(true);

        SubmitResponse response = service.submit(fixture.childId(), fixture.missionId(), fixture.request());

        assertThat(response.mode()).isEqualTo("review");
        assertThat(response.progressApplied()).isFalse();
        assertThat(response.isReview()).isTrue();

        ArgumentCaptor<MissionAttempt> attemptCaptor = ArgumentCaptor.forClass(MissionAttempt.class);
        verify(missionAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().isReview()).isTrue();
        assertThat(attemptCaptor.getValue().isPassed()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MissionAnswerResult>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(missionAnswerResultRepository).saveAll(resultsCaptor.capture());
        assertThat(resultsCaptor.getValue()).hasSize(10);
        assertThat(resultsCaptor.getValue()).allSatisfy(result -> {
            assertThat(result.isReview()).isTrue();
            assertThat(result.isCorrect()).isTrue();
        });
    }

    private SubmitService service() {
        return new SubmitService(
                quizAttemptRepository,
                missionRepository,
                questionBankRepository,
                questionAnswerKeyRepository,
                missionAnswerResultRepository,
                missionAttemptRepository,
                missionDailyProgressRepository,
                childProfileRepository,
                childActivityService,
                ticketRepository,
                streakRecordRepository,
                friendStreakRepository,
                milestoneRewardRepository,
                dailyQuestService,
                weeklyQuestService,
                achievementService,
                petGrowthService,
                quizService,
                missionService,
                new ObjectMapper()
        );
    }

    private Fixture fixture(boolean reviewMode) {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID quizAttemptId = UUID.randomUUID();
        StageProgressResponse stageProgress = new StageProgressResponse(0, 0, 0);
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        ChildProfile childProfile = org.mockito.Mockito.mock(ChildProfile.class);
        Ticket ticket = Ticket.create(childId, 2);
        StreakRecord streakRecord = StreakRecord.create(childId);

        when(mission.isActive()).thenReturn(true);
        when(mission.getId()).thenReturn(missionId);
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(missionService.getMissions(childId)).thenReturn(new MissionListResponse(List.of(), stageProgress));
        when(missionService.isUnlocked(mission, stageProgress)).thenReturn(true);

        List<UUID> questionIds = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> UUID.randomUUID())
                .toList();
        QuizAttempt attempt = QuizAttempt.create(childId, missionId, "[]", Instant.now().plusSeconds(600));
        setQuizAttemptId(attempt, quizAttemptId);
        when(quizAttemptRepository.findWithLockById(quizAttemptId)).thenReturn(Optional.of(attempt));
        when(quizService.parseQuestionIds("[]")).thenReturn(questionIds);

        List<QuestionBank> questionBanks = questionIds.stream()
                .map(id -> org.mockito.Mockito.mock(QuestionBank.class))
                .toList();
        when(questionBankRepository.findAllByIdIn(questionIds)).thenReturn(questionBanks);

        List<QuestionAnswerKey> answerKeys = questionIds.stream()
                .map(id -> QuestionAnswerKey.create(id, "\"No\"", "Explanation"))
                .toList();
        when(questionAnswerKeyRepository.findAllByQuestionIdIn(questionIds)).thenReturn(answerKeys);

        List<SubmitRequest.AnswerRequest> answers = questionIds.stream()
                .map(id -> new SubmitRequest.AnswerRequest(id.toString(), reviewMode ? "No" : "Yes"))
                .toList();
        SubmitRequest request = new SubmitRequest(quizAttemptId, answers);

        when(missionAttemptRepository.countByChildIdAndMissionIdAndAttemptDate(any(), any(), any()))
                .thenReturn(reviewMode ? 1L : 0L);
        when(missionAttemptRepository.save(any(MissionAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(missionAnswerResultRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        if (reviewMode) {
            MissionDailyProgress progress = org.mockito.Mockito.mock(MissionDailyProgress.class);
            when(missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(any(), any(), any()))
                    .thenReturn(Optional.of(progress));
            when(missionDailyProgressRepository.findWithLockByChildIdAndMissionIdAndProgressDate(any(), any(), any()))
                    .thenReturn(Optional.of(progress));
        } else {
            when(missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(any(), any(), any()))
                    .thenReturn(Optional.empty());
        }

        when(childProfileRepository.findById(childId)).thenReturn(Optional.of(childProfile));
        when(childProfile.getProfileImageType()).thenReturn(ProfileImageType.DEFAULT);
        when(ticketRepository.findWithLockByChildId(childId)).thenReturn(Optional.of(ticket));
        when(streakRecordRepository.findWithLockByChildId(childId)).thenReturn(Optional.of(streakRecord));

        return new Fixture(childId, missionId, request);
    }

    private void setQuizAttemptId(QuizAttempt attempt, UUID quizAttemptId) {
        try {
            Field idField = QuizAttempt.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(attempt, quizAttemptId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Fixture(
            UUID childId,
            UUID missionId,
            SubmitRequest request
    ) {
    }
}
