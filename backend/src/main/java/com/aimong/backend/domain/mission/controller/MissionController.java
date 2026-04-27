package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.dto.QuestionReportResponse;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.service.MissionService;
import com.aimong.backend.domain.mission.service.QuizService;
import com.aimong.backend.domain.mission.service.SubmitService;
import com.aimong.backend.domain.mission.service.question.QuestionQualityReviewService;
import com.aimong.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionController {

    private static final String CHILD_SECURITY = "bearerAuth";

    private final MissionService missionService;
    private final QuizService quizService;
    private final SubmitService submitService;
    private final QuestionQualityReviewService questionQualityReviewService;

    @Operation(
            summary = "미션 목록 조회",
            description = "전체 미션 목록과 단계별 진행 상태를 조회합니다",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "missions": [
                          {
                            "id": "3f1a2b4c-1111-2222-3333-444455556666",
                            "stage": 1,
                            "title": "AI가 뭐예요?",
                            "description": "AI의 개념과 할루시네이션을 배워요",
                            "isUnlocked": true,
                            "isCompleted": true,
                            "completedAt": "2026-03-28",
                            "isReviewable": true
                          }
                        ],
                        "stageProgress": {
                          "stage1Completed": 3,
                          "stage2Completed": 0,
                          "stage3Completed": 0
                        }
                      },
                      "requestId": "req_01HXYZABC123"
                    }
                    """))
    )
    @GetMapping
    public ApiResponse<MissionListResponse> getMissions(Authentication authentication) {
        return ApiResponse.success(missionService.getMissions(extractChildId(authentication)));
    }

    @Operation(
            summary = "미션 문제 조회",
            description = "승인된 고정 문제 10문항과 quizAttemptId를 반환합니다",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "문제 조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "missionId": "3f1a2b4c-1111-2222-3333-444455556666",
                                "missionTitle": "AI가 뭐예요?",
                                "isReview": false,
                                "quizAttemptId": "8e8c7d6a-1111-2222-3333-444455556666",
                                "questionCount": 10,
                                "expiresAt": "2026-03-29T10:00:00Z",
                                "questions": [
                                  {
                                    "id": "7d7c6b5a-1111-2222-3333-444455556666",
                                    "type": "OX",
                                    "question": "AI는 항상 정확한 정보를 말한다.",
                                    "options": null
                                  }
                                ]
                              },
                              "requestId": "req_01HXYZABC123"
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "잠긴 미션",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "error": {
                                "code": "FORBIDDEN",
                                "message": "아직 잠긴 미션이에요. 이전 단계를 먼저 완료해주세요"
                              },
                              "requestId": "req_01HXYZABC123"
                            }
                            """))
            )
    })
    @GetMapping("/{missionId}/questions")
    public ApiResponse<MissionQuestionsResponse> getQuestions(
            @PathVariable UUID missionId,
            Authentication authentication
    ) {
        return ApiResponse.success(quizService.getQuestions(extractChildId(authentication), missionId));
    }

    @Operation(
            summary = "미션 정답 제출",
            description = "10문항 답안을 제출하고 채점 결과와 보상 정보를 반환합니다",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "quizAttemptId": "8e8c7d6a-1111-2222-3333-444455556666",
                      "answers": [
                        { "questionId": "q_001", "selected": "false" },
                        { "questionId": "q_002", "selected": "없는 정보를 사실처럼 말함" },
                        { "questionId": "q_003", "selected": "프롬프트" },
                        { "questionId": "q_004", "selected": "AI에게 힌트만 받도록 권유한다" },
                        { "questionId": "q_005", "selected": "다른 자료로 확인한다" },
                        { "questionId": "q_006", "selected": "No" },
                        { "questionId": "q_007", "selected": "No" },
                        { "questionId": "q_008", "selected": "No" },
                        { "questionId": "q_009", "selected": "No" },
                        { "questionId": "q_010", "selected": "No" }
                      ]
                    }
                    """))
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "제출 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "mode": "normal",
                                "progressApplied": true,
                                "attemptState": "submitted",
                                "score": 8,
                                "total": 10,
                                "wrongCount": 2,
                                "isPassed": true,
                                "isPerfect": false,
                                "equippedPetGrade": "EPIC",
                                "bonusXp": 10,
                                "bonusReason": "PET_RARITY_BONUS",
                                "xpEarned": 20,
                                "equippedPetXp": 95,
                                "petStage": "GROWTH",
                                "petEvolved": false,
                                "crownUnlocked": false,
                                "crownType": null,
                                "streakDays": 5,
                                "todayMissionCount": 1,
                                "streakBonusApplied": false,
                                "rewards": [],
                                "remainingTickets": {
                                  "normal": 2,
                                  "rare": 0,
                                  "epic": 1
                                },
                                "profileImageType": "SPROUT",
                                "profileImageUnlocked": false,
                                "isReview": false,
                                "results": [
                                  {
                                    "questionId": "q_001",
                                    "isCorrect": true,
                                    "explanation": "AI는 할루시네이션이 발생할 수 있어요"
                                  }
                                ]
                              },
                              "requestId": "req_01HXYZABC123"
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "답안 검증 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "quiz_attempt_required", value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "BAD_REQUEST",
                                        "message": "문제 세션 정보가 필요합니다"
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """),
                            @ExampleObject(name = "answers_required", value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "BAD_REQUEST",
                                        "message": "답안은 10개를 모두 제출해주세요"
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """),
                            @ExampleObject(name = "duplicate_question", value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "BAD_REQUEST",
                                        "message": "같은 문제를 중복 제출할 수 없어요"
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """)
                    })
            )
    })
    @PostMapping("/{missionId}/submit")
    public ApiResponse<SubmitResponse> submit(
            @PathVariable UUID missionId,
            @Valid @RequestBody SubmitRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(submitService.submit(extractChildId(authentication), missionId, request));
    }

    @PostMapping("/{missionId}/questions/{questionId}/report")
    public ApiResponse<QuestionReportResponse> reportQuestion(
            @PathVariable UUID missionId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionReportRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(questionQualityReviewService.reportQuestion(
                extractChildId(authentication),
                missionId,
                questionId,
                request
        ));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
