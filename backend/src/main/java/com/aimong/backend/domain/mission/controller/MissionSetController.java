package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.MissionSetCheckRequest;
import com.aimong.backend.domain.mission.dto.QuestionCheckResponse;
import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.dto.QuestionReportResponse;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.service.QuestionCheckService;
import com.aimong.backend.domain.mission.service.QuizService;
import com.aimong.backend.domain.mission.service.SubmitService;
import com.aimong.backend.domain.mission.service.question.QuestionQualityReviewService;
import com.aimong.backend.global.response.ApiResponse;
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
@RequestMapping("/mission-sets")
public class MissionSetController {

    private final QuizService quizService;
    private final SubmitService submitService;
    private final QuestionCheckService questionCheckService;
    private final QuestionQualityReviewService questionQualityReviewService;

    @GetMapping("/{setId}/questions")
    public ApiResponse<MissionQuestionsResponse> getQuestions(
            @PathVariable String setId,
            Authentication authentication
    ) {
        return ApiResponse.success(quizService.getQuestions(extractChildId(authentication), setId));
    }

    @PostMapping("/{setId}/submit")
    public ApiResponse<SubmitResponse> submit(
            @PathVariable String setId,
            @Valid @RequestBody SubmitRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(submitService.submit(extractChildId(authentication), setId, request));
    }

    @PostMapping("/{setId}/check")
    public ApiResponse<QuestionCheckResponse> checkQuestion(
            @PathVariable String setId,
            @Valid @RequestBody MissionSetCheckRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(questionCheckService.check(extractChildId(authentication), setId, request));
    }

    @PostMapping("/{setId}/questions/{questionId}/report")
    public ApiResponse<QuestionReportResponse> reportQuestion(
            @PathVariable String setId,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionReportRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(questionQualityReviewService.reportQuestion(
                extractChildId(authentication),
                setId,
                questionId,
                request
        ));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
