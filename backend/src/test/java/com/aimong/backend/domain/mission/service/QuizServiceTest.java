package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionSummaryResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.mission.service.question.AsyncMissionRefillService;
import com.aimong.backend.domain.mission.service.question.MissionQuestionSetFactory;
import com.aimong.backend.domain.mission.service.question.QuestionServingQualityGuard;
import com.aimong.backend.global.util.KstDateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuizServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private MissionAttemptRepository missionAttemptRepository;

    @Mock
    private MissionDailyProgressRepository missionDailyProgressRepository;

    @Mock
    private MissionService missionService;

    @Mock
    private ChildActivityService childActivityService;

    @Mock
    private MissionQuestionSetFactory missionQuestionSetFactory;

    @Mock
    private AsyncMissionRefillService asyncMissionRefillService;

    @Mock
    private QuestionServingQualityGuard questionServingQualityGuard;

    private final MissionQuestionProperties missionQuestionProperties = new MissionQuestionProperties(1, 30, false);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getQuestionsMarksOnlyCompletedMissionAsReview() {
        QuizService quizService = quizService();

        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        QuestionBank question = question("AI can be wrong.");

        wireUnlockedMission(childId, missionId, mission);
        when(missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(childId, missionId, KstDateUtils.today()))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(MissionDailyProgress.class)));
        when(missionQuestionSetFactory.create(missionId, childId, true)).thenReturn(List.of(question));
        when(questionServingQualityGuard.validateForServing(mission, List.of(question)))
                .thenReturn(new QuestionServingQualityGuard.ServingValidationResult(List.of(question), List.of()));

        assertThat(quizService.getQuestions(childId, missionId).isReview()).isTrue();
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void servingValidationRetrySelectsReplacementQuestion() {
        QuizService quizService = quizService();

        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        QuestionBank rejectedQuestion = question("old question");
        QuestionBank replacementQuestion = question("replacement question");
        UUID rejectedQuestionId = rejectedQuestion.getId();

        wireUnlockedMission(childId, missionId, mission);
        when(missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(childId, missionId, KstDateUtils.today()))
                .thenReturn(Optional.empty());
        when(missionQuestionSetFactory.create(missionId, childId, false))
                .thenReturn(List.of(rejectedQuestion))
                .thenReturn(List.of(replacementQuestion));
        when(questionServingQualityGuard.validateForServing(mission, List.of(rejectedQuestion)))
                .thenReturn(new QuestionServingQualityGuard.ServingValidationResult(List.of(), List.of(rejectedQuestionId)));
        when(questionServingQualityGuard.validateForServing(mission, List.of(replacementQuestion)))
                .thenReturn(new QuestionServingQualityGuard.ServingValidationResult(List.of(replacementQuestion), List.of()));

        assertThat(quizService.getQuestions(childId, missionId).questions())
                .extracting(questionResponse -> questionResponse.question())
                .containsExactly("replacement question");
        verify(missionQuestionSetFactory, times(2)).create(missionId, childId, false);
    }

    private QuizService quizService() {
        return new QuizService(
                missionRepository,
                quizAttemptRepository,
                missionAttemptRepository,
                missionDailyProgressRepository,
                childActivityService,
                missionService,
                missionQuestionSetFactory,
                asyncMissionRefillService,
                missionQuestionProperties,
                questionServingQualityGuard,
                objectMapper
        );
    }

    private void wireUnlockedMission(UUID childId, UUID missionId, Mission mission) {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(mission.isActive()).thenReturn(true);
        when(mission.getId()).thenReturn(missionId);
        when(mission.getTitle()).thenReturn("AI Basics");
        StageProgressResponse stageProgress = new StageProgressResponse(0, 0, 0);
        when(missionService.getMissions(childId))
                .thenReturn(new MissionListResponse(
                        List.of(new MissionSummaryResponse(missionId, (short) 1, "AI Basics", null, true, false, null, false)),
                        stageProgress
                ));
        when(missionService.isUnlocked(mission, stageProgress)).thenReturn(true);
    }

    private QuestionBank question(String prompt) {
        QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);
        when(question.getId()).thenReturn(UUID.randomUUID());
        when(question.getQuestionType()).thenReturn(QuestionType.OX);
        when(question.getPrompt()).thenReturn(prompt);
        return question;
    }
}
