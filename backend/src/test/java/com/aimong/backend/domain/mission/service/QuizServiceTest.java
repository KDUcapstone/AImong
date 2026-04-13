package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionSummaryResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.mission.service.question.MissionQuestionSetFactory;
import com.aimong.backend.global.util.KstDateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private MissionAttemptRepository missionAttemptRepository;

    @Mock
    private MissionService missionService;

    @Mock
    private MissionQuestionSetFactory missionQuestionSetFactory;

    private final MissionQuestionProperties missionQuestionProperties = new MissionQuestionProperties(10, 30, false);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getQuestionsMarksSecondSameDayAttemptAsReviewEvenIfFirstAttemptFailed() {
        QuizService quizService = new QuizService(
                missionRepository,
                quizAttemptRepository,
                missionAttemptRepository,
                missionService,
                missionQuestionSetFactory,
                missionQuestionProperties,
                objectMapper
        );

        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        QuestionBank question = org.mockito.Mockito.mock(QuestionBank.class);

        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(mission.isActive()).thenReturn(true);
        when(mission.getId()).thenReturn(missionId);
        when(mission.getTitle()).thenReturn("AI Basics");
        when(missionService.getMissions(childId))
                .thenReturn(new MissionListResponse(
                        List.of(new MissionSummaryResponse(missionId, (short) 1, "AI Basics", null, true, false, null, false)),
                        new StageProgressResponse(0, 0, 0)
                ));
        when(missionService.isUnlocked(mission, new StageProgressResponse(0, 0, 0))).thenReturn(true);
        when(missionAttemptRepository.existsByChildIdAndMissionIdAndAttemptDate(childId, missionId, KstDateUtils.today()))
                .thenReturn(true);
        when(missionQuestionSetFactory.create(missionId, childId, true)).thenReturn(List.of(question));
        when(question.getId()).thenReturn(UUID.randomUUID());
        when(question.getQuestionType()).thenReturn(com.aimong.backend.domain.mission.entity.QuestionType.OX);
        when(question.getPrompt()).thenReturn("AI can be wrong.");

        assertThat(quizService.getQuestions(childId, missionId).isReview()).isTrue();
    }
}
