package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.AbandonAttemptRequest;
import com.aimong.backend.domain.mission.dto.AbandonAttemptResponse;
import com.aimong.backend.domain.mission.dto.QuizAttemptResponse;
import com.aimong.backend.domain.mission.dto.ReviveAttemptRequest;
import com.aimong.backend.domain.mission.dto.ReviveAttemptResponse;
import com.aimong.backend.domain.mission.service.QuizAttemptService;
import com.aimong.backend.global.response.ApiResponse;
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
@RequestMapping("/mission-attempts")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @GetMapping("/{attemptId}")
    public ApiResponse<QuizAttemptResponse> getAttempt(
            @PathVariable UUID attemptId,
            Authentication authentication
    ) {
        return ApiResponse.success(quizAttemptService.getAttempt(extractChildId(authentication), attemptId));
    }

    @PostMapping("/{attemptId}/abandon")
    public ApiResponse<AbandonAttemptResponse> abandon(
            @PathVariable UUID attemptId,
            @RequestBody(required = false) AbandonAttemptRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(quizAttemptService.abandon(extractChildId(authentication), attemptId, request));
    }

    @PostMapping("/{attemptId}/revive")
    public ApiResponse<ReviveAttemptResponse> revive(
            @PathVariable UUID attemptId,
            @RequestBody ReviveAttemptRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(quizAttemptService.revive(extractChildId(authentication), attemptId, request));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
