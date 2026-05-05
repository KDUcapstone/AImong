package com.aimong.backend.domain.parent.controller;

import com.aimong.backend.domain.parent.dto.ParentChildSummaryResponse;
import com.aimong.backend.domain.parent.dto.ParentPrivacyLogResponse;
import com.aimong.backend.domain.parent.dto.ParentWeakPointsResponse;
import com.aimong.backend.domain.parent.dto.ParentWeeklyStatsResponse;
import com.aimong.backend.domain.parent.service.ParentDashboardService;
import com.aimong.backend.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/parent/child/{childId}")
public class ParentDashboardController {

    private final ParentDashboardService parentDashboardService;

    @GetMapping("/summary")
    public ApiResponse<ParentChildSummaryResponse> getSummary(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        return ApiResponse.success(parentDashboardService.getSummary(authentication.getName(), childId));
    }

    @GetMapping("/weekly-stats")
    public ApiResponse<ParentWeeklyStatsResponse> getWeeklyStats(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        return ApiResponse.success(parentDashboardService.getWeeklyStats(authentication.getName(), childId));
    }

    @GetMapping("/privacy-log")
    public ApiResponse<ParentPrivacyLogResponse> getPrivacyLog(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.success(parentDashboardService.getPrivacyLog(
                authentication.getName(),
                childId,
                page,
                size
        ));
    }

    @GetMapping("/weak-points")
    public ApiResponse<ParentWeakPointsResponse> getWeakPoints(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.success(parentDashboardService.getWeakPoints(
                authentication.getName(),
                childId,
                page,
                size
        ));
    }
}
