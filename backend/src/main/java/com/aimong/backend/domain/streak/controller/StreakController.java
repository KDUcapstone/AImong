package com.aimong.backend.domain.streak.controller;

import com.aimong.backend.domain.streak.dto.StreakResponse;
import com.aimong.backend.domain.streak.service.StreakService;
import com.aimong.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/streak")
public class StreakController {

    private static final String CHILD_SECURITY = "bearerAuth";

    private final StreakService streakService;

    @Operation(
            summary = "스트릭 현황 조회",
            description = "자녀의 연속 학습일, 오늘 완료 미션 수, 보호권 정보를 조회합니다.",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @GetMapping
    public ApiResponse<StreakResponse> getStreak(Authentication authentication) {
        return ApiResponse.success(streakService.getStreak(UUID.fromString(authentication.getName())));
    }
}
