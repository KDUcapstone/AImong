package com.aimong.backend.domain.streak.controller;

import com.aimong.backend.domain.streak.dto.PartnerConnectRequest;
import com.aimong.backend.domain.streak.dto.PartnerConnectResponse;
import com.aimong.backend.domain.streak.dto.PartnerDisconnectResponse;
import com.aimong.backend.domain.streak.dto.StreakResponse;
import com.aimong.backend.domain.streak.service.StreakService;
import com.aimong.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(
            summary = "공동 스트릭 파트너 연결",
            description = "상대 자녀의 6자리 코드로 공동 스트릭 파트너를 연결합니다.",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @PostMapping("/partner")
    public ApiResponse<PartnerConnectResponse> connectPartner(
            Authentication authentication,
            @Valid @RequestBody PartnerConnectRequest request
    ) {
        return ApiResponse.success(streakService.connectPartner(
                UUID.fromString(authentication.getName()),
                request.partnerCode()
        ));
    }

    @Operation(
            summary = "공동 스트릭 파트너 해제",
            description = "현재 연결된 공동 스트릭 파트너를 해제합니다.",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @DeleteMapping("/partner")
    public ApiResponse<PartnerDisconnectResponse> disconnectPartner(Authentication authentication) {
        return ApiResponse.success(streakService.disconnectPartner(UUID.fromString(authentication.getName())));
    }
}
