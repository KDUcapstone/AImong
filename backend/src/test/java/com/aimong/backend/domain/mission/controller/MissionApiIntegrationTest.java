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
import com.aimong.backend.domain.mission.dto.MissionSummaryResponse;
import com.aimong.backend.domain.mission.dto.QuestionResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.service.MissionService;
import com.aimong.backend.domain.mission.service.QuizService;
import com.aimong.backend.domain.mission.service.SubmitService;
import com.aimong.backend.global.filter.FirebaseParentAuthFilter;
import com.aimong.backend.global.filter.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MissionController.class)
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
    private FirebaseParentAuthFilter firebaseParentAuthFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getQuestionsReturnsQuizAttemptContract() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID quizAttemptId = UUID.randomUUID();
        MissionQuestionsResponse response = new MissionQuestionsResponse(
                missionId,
                "Privacy Safety",
                true,
                quizAttemptId,
                10,
                Instant.parse("2026-04-14T12:00:00Z"),
                List.of(new QuestionResponse(UUID.randomUUID(), "MCQ", "Should you share a password?", List.of("Yes", "No")))
        );

        given(quizService.getQuestions(childId, missionId)).willReturn(response);

        mockMvc.perform(get("/missions/{missionId}/questions", missionId)
                        .principal(new UsernamePasswordAuthenticationToken(
                                childId.toString(),
                                null,
                                Collections.emptyList()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.missionId").value(missionId.toString()))
                .andExpect(jsonPath("$.data.missionTitle").value("Privacy Safety"))
                .andExpect(jsonPath("$.data.isReview").value(true))
                .andExpect(jsonPath("$.data.quizAttemptId").value(quizAttemptId.toString()))
                .andExpect(jsonPath("$.data.questionCount").value(10))
                .andExpect(jsonPath("$.data.questions[0].type").value("MCQ"));
    }

    @Test
    void submitReturnsMissionResultContract() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID quizAttemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SubmitRequest request = new SubmitRequest(
                quizAttemptId,
                List.of(new SubmitRequest.AnswerRequest(questionId.toString(), "No"))
        );
        SubmitResponse response = new SubmitResponse(
                "normal",
                true,
                "submitted",
                10,
                10,
                0,
                true,
                true,
                "NORMAL",
                2,
                "PET_GRADE_BONUS",
                12,
                1,
                2,
                true,
                50,
                10,
                "BABY",
                false,
                false,
                null,
                3,
                List.of(new SubmitResponse.RewardResponse("TICKET", "NORMAL", 1, null, "LEVEL_UP")),
                List.of(new SubmitResponse.RewardResponse("XP", null, null, 12, "MISSION_CLEAR")),
                List.of(new SubmitResponse.ResultResponse(questionId.toString(), true, "Do not share passwords."))
        );

        given(submitService.submit(eq(childId), eq(missionId), any(SubmitRequest.class))).willReturn(response);

        mockMvc.perform(post("/missions/{missionId}/submit", missionId)
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
                .andExpect(jsonPath("$.data.mode").value("normal"))
                .andExpect(jsonPath("$.data.progressApplied").value(true))
                .andExpect(jsonPath("$.data.attemptState").value("submitted"))
                .andExpect(jsonPath("$.data.score").value(10))
                .andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.levelRewards[0].ticketType").value("NORMAL"))
                .andExpect(jsonPath("$.data.rewards[0].amount").value(12))
                .andExpect(jsonPath("$.data.results[0].questionId").value(questionId.toString()));
    }

    @Test
    void getMissionsReturnsMissionListEnvelope() throws Exception {
        UUID childId = UUID.randomUUID();
        MissionListResponse response = new MissionListResponse(
                List.of(new MissionSummaryResponse(UUID.randomUUID(), 1, "Password", "Read the prompt", true, false, null, false)),
                new StageProgressResponse(1, 0, 0)
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
                .andExpect(jsonPath("$.data.missions[0].title").value("Password"))
                .andExpect(jsonPath("$.data.stageProgress.stage1Completed").value(1));
    }
}
