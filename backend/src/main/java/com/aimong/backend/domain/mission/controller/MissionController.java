package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.QuestionReportRequest;
import com.aimong.backend.domain.mission.dto.QuestionReportResponse;
import com.aimong.backend.domain.mission.service.MissionService;
import com.aimong.backend.domain.mission.service.QuizService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionController {

    private final MissionService missionService;
    private final QuizService quizService;
    private final QuestionQualityReviewService questionQualityReviewService;

    @GetMapping
    public ApiResponse<MissionListResponse> getMissions(Authentication authentication) {
        return ApiResponse.success(missionService.getMissions(extractChildId(authentication)));
    }

    @GetMapping("/{missionId}/questions")
    public ApiResponse<MissionQuestionsResponse> getQuestions(
            @PathVariable UUID missionId,
            @RequestParam(defaultValue = "1") int starLevel,
            Authentication authentication
    ) {
        return ApiResponse.success(quizService.getQuestions(extractChildId(authentication), missionId, starLevel));
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
