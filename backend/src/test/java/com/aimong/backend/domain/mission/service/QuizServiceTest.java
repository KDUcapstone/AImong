package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.mission.service.question.MissionQuestionSetFactory;
import com.aimong.backend.domain.mission.service.question.postmvp.AsyncMissionRefillService;
import com.aimong.backend.domain.mission.service.question.postmvp.ValidatedDynamicQuestionGenerationPort;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuizServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private MissionDailyProgressRepository missionDailyProgressRepository;

    @Mock
    private ChildActivityService childActivityService;

    @Mock
    private MissionService missionService;

    @Mock
    private MissionQuestionSetFactory missionQuestionSetFactory;

    private final MissionQuestionProperties missionQuestionProperties =
            new MissionQuestionProperties(10, 30, false, false, false, false);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getQuestionsReturnsReviewFlagAndDoesNotExposeAnswerFields() {
        QuizService quizService = quizService();
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        Mission mission = mission(missionId);
        List<QuestionBank> questions = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> question("Should you share a password? " + index))
                .toList();

        wireUnlockedMission(childId, missionId, mission);
        when(missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(childId, missionId, KstDateUtils.today()))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(MissionDailyProgress.class)));
        when(missionQuestionSetFactory.create(missionId, childId, true)).thenReturn(questions);
        when(quizAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = quizService.getQuestions(childId, missionId);

        assertThat(response.isReview()).isTrue();
        assertThat(response.questions()).hasSize(10);
        assertThat(response.questions().getFirst().type()).isEqualTo("OX");
        assertThat(response.questions().getFirst().question()).startsWith("Should you share a password?");
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void shortageReturnsMissionSetNotReadyWithoutCreatingAttempt() {
        QuizService quizService = quizService();
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        Mission mission = mission(missionId);

        wireUnlockedMission(childId, missionId, mission);
        when(missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(childId, missionId, KstDateUtils.today()))
                .thenReturn(Optional.empty());
        when(missionQuestionSetFactory.create(missionId, childId, false))
                .thenThrow(new AimongException(ErrorCode.MISSION_SET_NOT_READY));

        assertThatThrownBy(() -> quizService.getQuestions(childId, missionId))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSION_SET_NOT_READY);

        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    void quizServiceDoesNotDependOnPostMvpGenerationComponents() {
        List<Class<?>> fieldTypes = Arrays.stream(QuizService.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)
                .toList();

        assertThat(fieldTypes).doesNotContain(
                ValidatedDynamicQuestionGenerationPort.class,
                AsyncMissionRefillService.class,
                QuestionGenerationService.class
        );
    }

    private QuizService quizService() {
        return new QuizService(
                missionRepository,
                quizAttemptRepository,
                missionDailyProgressRepository,
                childActivityService,
                missionService,
                missionQuestionSetFactory,
                missionQuestionProperties,
                objectMapper
        );
    }

    private void wireUnlockedMission(UUID childId, UUID missionId, Mission mission) {
        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        StageProgressResponse stageProgress = new StageProgressResponse(0, 0, 0);
        when(missionService.getMissions(childId))
                .thenReturn(new MissionListResponse(
                        List.of(new MissionSummaryResponse(missionId, (short) 1, "AI Basics", null, true, false, null, false)),
                        stageProgress
                ));
        when(missionService.isUnlocked(mission, stageProgress)).thenReturn(true);
    }

    private Mission mission(UUID missionId) {
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        when(mission.isActive()).thenReturn(true);
        when(mission.getId()).thenReturn(missionId);
        when(mission.getTitle()).thenReturn("AI Basics");
        return mission;
    }

    private QuestionBank question(String prompt) {
        QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);
        when(question.getId()).thenReturn(UUID.randomUUID());
        when(question.getQuestionType()).thenReturn(QuestionType.OX);
        when(question.getPrompt()).thenReturn(prompt);
        when(question.getOptionsJson()).thenReturn(null);
        return question;
    }
}
