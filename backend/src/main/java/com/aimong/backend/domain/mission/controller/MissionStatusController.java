package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.MissionStatusResponse;
import com.aimong.backend.domain.mission.service.QuizAttemptService;
import com.aimong.backend.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionStatusController {

    private final QuizAttemptService quizAttemptService;

    @GetMapping("/{missionId}/status")
    public ApiResponse<MissionStatusResponse> getStatus(
            @PathVariable UUID missionId,
            Authentication authentication
    ) {
        return ApiResponse.success(quizAttemptService.getMissionStatus(extractChildId(authentication), missionId));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
