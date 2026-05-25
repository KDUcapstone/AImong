package com.aimong.backend.domain.mission.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.MissionSetCheckRequest;
import com.aimong.backend.domain.mission.dto.QuestionCheckResponse;
import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.dto.QuestionReportResponse;
import com.aimong.backend.domain.mission.dto.QuestionResponse;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.service.MissionService;
import com.aimong.backend.domain.mission.service.MissionSetReportService;
import com.aimong.backend.domain.mission.service.QuestionCheckService;
import com.aimong.backend.domain.mission.service.QuizService;
import com.aimong.backend.domain.mission.service.SubmitService;
import com.aimong.backend.domain.mission.service.question.QuestionQualityReviewService;
import com.aimong.backend.global.filter.FirebaseParentAuthFilter;
import com.aimong.backend.global.filter.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({MissionController.class, MissionSetController.class})
@AutoConfigureMockMvc(addFilters = false)
class MissionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MissionService missionService;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private SubmitService submitService;

    @MockitoBean
    private QuestionCheckService questionCheckService;

    @MockitoBean
    private MissionSetReportService missionSetReportService;

    @MockitoBean
    private QuestionQualityReviewService questionQualityReviewService;

    @MockitoBean
    private FirebaseParentAuthFilter firebaseParentAuthFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getQuestionsReturnsQuizAttemptContract() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID quizAttemptId = UUID.randomUUID();
        MissionQuestionsResponse response = new MissionQuestionsResponse(
                "S0101-L1",
                missionId,
                "S0101",
                1,
                1,
                MissionQuestionsResponse.labelForStar(1),
                true,
                0,
                null,
                null,
                quizAttemptId,
                10,
                List.of(new QuestionResponse(UUID.randomUUID(), "OX", "Should you share a password?", List.of("Yes", "No")))
        );

        given(quizService.getQuestions(childId, missionId, 1)).willReturn(response);

        mockMvc.perform(get("/missions/{missionId}/questions", missionId)
                        .principal(new UsernamePasswordAuthenticationToken(
                                childId.toString(),
                                null,
                                Collections.emptyList()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.missionId").value(missionId.toString()))
                .andExpect(jsonPath("$.data.starLevel").value(1))
                .andExpect(jsonPath("$.data.variantNo").value(1))
                .andExpect(jsonPath("$.data.stage").doesNotExist())
                .andExpect(jsonPath("$.data.title").doesNotExist())
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.data.isReview").value(true))
                .andExpect(jsonPath("$.data.attemptId").value(quizAttemptId.toString()))
                .andExpect(jsonPath("$.data.quizAttemptId").doesNotExist())
                .andExpect(jsonPath("$.data.questionCount").value(10))
                .andExpect(jsonPath("$.data.questions[0].questionId").exists())
                .andExpect(jsonPath("$.data.questions[0].questionNo").value(1))
                .andExpect(jsonPath("$.data.questions[0].type").value("OX"))
                .andExpect(jsonPath("$.data.questions[0].prompt").value("Should you share a password?"))
                .andExpect(jsonPath("$.data.questions[0].choices[0]").value("Yes"))
                .andExpect(jsonPath("$.data.questions[0].answerFormat").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.data.questions[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].question").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].answerPayload").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].answer_payload").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].correctIndex").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].answerKey").doesNotExist());
    }

    @Test
    void submitReturnsMissionResultContract() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID quizAttemptId = UUID.randomUUID();
        List<SubmitRequest.AnswerRequest> answers = IntStream.range(0, 10)
                .mapToObj(index -> new SubmitRequest.AnswerRequest(UUID.randomUUID().toString(), "No"))
                .toList();
        SubmitRequest request = new SubmitRequest(quizAttemptId, answers);
        SubmitResponse response = new SubmitResponse(
                "normal",
                true,
                "submitted",
                quizAttemptId,
                100,
                10,
                10,
                10,
                true,
                0,
                true,
                true,
                "NORMAL",
                0,
                null,
                10,
                95,
                "GROWTH",
                false,
                false,
                null,
                3,
                1,
                false,
                new SubmitResponse.RewardsResponse(30, 10, List.of()),
                new SubmitResponse.RemainingTicketsResponse(2),
                "SPROUT",
                false,
                false,
                List.of(new SubmitResponse.ResultResponse(answers.get(0).questionId(), true, "Do not share passwords.")),
                "S0101-L1",
                missionId.toString(),
                1,
                1,
                1,
                1,
                List.of(),
                1,
                null
        );

        given(submitService.submit(eq(childId), eq("S0101-L1"), any(SubmitRequest.class))).willReturn(response);

        mockMvc.perform(post("/mission-sets/{setId}/submit", "S0101-L1")
                        .principal(new UsernamePasswordAuthenticationToken(
                                childId.toString(),
                                null,
                                Collections.emptyList()
                        ))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.mode").value("normal"))
                .andExpect(jsonPath("$.data.progressApplied").value(true))
                .andExpect(jsonPath("$.data.isFirstClear").value(true))
                .andExpect(jsonPath("$.data.attemptState").value("submitted"))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.todayMissionCount").value(1))
                .andExpect(jsonPath("$.data.streakBonusApplied").value(false))
                .andExpect(jsonPath("$.data.rewards.gear").value(30))
                .andExpect(jsonPath("$.data.rewards.exp").value(10))
                .andExpect(jsonPath("$.data.rewards.fragments").isArray())
                .andExpect(jsonPath("$.data.remainingTickets.normal").value(2))
                .andExpect(jsonPath("$.data.profileImageType").value("SPROUT"))
                .andExpect(jsonPath("$.data.results[0].questionId").value(answers.get(0).questionId()))
                .andExpect(jsonPath("$.data.results[0].explanation").value("Do not share passwords."));
    }

    @Test
    void checkMissionSetQuestionReturnsImmediateFeedbackWithoutSubmitContract() throws Exception {
        UUID childId = UUID.randomUUID();
        String setId = "S0101-L1";
        UUID questionId = UUID.randomUUID();
        MissionSetCheckRequest request = new MissionSetCheckRequest(questionId.toString(), "No");
        QuestionCheckResponse response = new QuestionCheckResponse(questionId, true, "No", "Do not share passwords.");

        given(questionCheckService.check(eq(childId), eq(setId), any(MissionSetCheckRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/mission-sets/{setId}/check", setId)
                        .principal(new UsernamePasswordAuthenticationToken(
                                childId.toString(),
                                null,
                                Collections.emptyList()
                        ))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.data.isCorrect").value(true))
                .andExpect(jsonPath("$.data.explanation").value("Do not share passwords."))
                .andExpect(jsonPath("$.data.xpEarned").doesNotExist())
                .andExpect(jsonPath("$.data.rewards").doesNotExist());
    }

    @Test
    void reportQuestionUsesMissionBasedApiPath() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID issueId = UUID.randomUUID();
        QuestionReportRequest request = new QuestionReportRequest("SAFETY", "Not suitable");
        QuestionReportResponse response = new QuestionReportResponse(questionId, issueId, "OPEN", false);

        given(questionQualityReviewService.reportQuestion(
                eq(childId),
                eq(missionId),
                eq(questionId),
                any(QuestionReportRequest.class)
        )).willReturn(response);

        mockMvc.perform(post("/missions/{missionId}/questions/{questionId}/report", missionId, questionId)
                        .principal(new UsernamePasswordAuthenticationToken(
                                childId.toString(),
                                null,
                                Collections.emptyList()
                        ))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.data.issueId").value(issueId.toString()))
                .andExpect(jsonPath("$.data.issueStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.quarantined").value(false));
    }

    @Test
    void getMissionsReturnsMissionListEnvelope() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        MissionListResponse response = new MissionListResponse(
                List.of(new MissionListResponse.StageResponse(
                        1,
                        "Stage 1",
                        List.of(new MissionListResponse.MissionResponse(
                                missionId,
                                "S0101",
                                "Password",
                                "Read the prompt",
                                true,
                                List.of(new MissionListResponse.StarLevelResponse(
                                        1,
                                        MissionListResponse.labelForStar(1),
                                        1,
                                        1,
                                        true,
                                        true
                                )),
                                1
                        ))
                )),
                new MissionListResponse.ProgressResponse(1, 1)
        );

        given(missionService.getMissions(childId)).willReturn(response);

        mockMvc.perform(get("/missions")
                        .principal(new UsernamePasswordAuthenticationToken(
                                childId.toString(),
                                null,
                                Collections.emptyList()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.stages[0].missions[0].title").value("Password"))
                .andExpect(jsonPath("$.data.progress.completedSetCount").value(1));
    }
}
