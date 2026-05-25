package com.aimong.backend.domain.stagereward.controller;

import com.aimong.backend.domain.stagereward.dto.CreateStageRewardRequest;
import com.aimong.backend.domain.stagereward.dto.StageRewardListResponse;
import com.aimong.backend.domain.stagereward.dto.StageRewardResponse;
import com.aimong.backend.domain.stagereward.dto.UpdateStageRewardRequest;
import com.aimong.backend.domain.stagereward.service.StageCompletionRewardService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StageCompletionRewardController {

    private final StageCompletionRewardService stageCompletionRewardService;

    @PostMapping("/parent/children/{childId}/stage-rewards")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StageRewardResponse> createStageReward(
            @PathVariable UUID childId,
            @Valid @RequestBody CreateStageRewardRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(stageCompletionRewardService.createStageReward(
                authentication.getName(),
                childId,
                request
        ));
    }

    @PatchMapping("/parent/children/{childId}/stage-rewards/{stageNumber}")
    public ApiResponse<StageRewardResponse> updateStageReward(
            @PathVariable UUID childId,
            @PathVariable int stageNumber,
            @Valid @RequestBody UpdateStageRewardRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(stageCompletionRewardService.updateStageReward(
                authentication.getName(),
                childId,
                stageNumber,
                request
        ));
    }

    @GetMapping("/parent/children/{childId}/stage-rewards")
    public ApiResponse<StageRewardListResponse> getStageRewards(
            @PathVariable UUID childId,
            Authentication authentication
    ) {
        return ApiResponse.success(stageCompletionRewardService.getStageRewards(authentication.getName(), childId));
    }
}
