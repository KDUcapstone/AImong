package com.aimong.backend.domain.reward.controller;

import com.aimong.backend.domain.reward.dto.ReturnRewardClaimResponse;
import com.aimong.backend.domain.reward.dto.ReturnRewardResponse;
import com.aimong.backend.domain.reward.service.ReturnRewardService;
import com.aimong.backend.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/return-reward")
public class ReturnRewardController {

    private final ReturnRewardService returnRewardService;

    @GetMapping
    public ApiResponse<ReturnRewardResponse> getReturnReward(Authentication authentication) {
        return ApiResponse.success(returnRewardService.getReturnReward(extractChildId(authentication)));
    }

    @PostMapping("/claim")
    public ApiResponse<ReturnRewardClaimResponse> claimReturnReward(Authentication authentication) {
        return ApiResponse.success(returnRewardService.claimReturnReward(extractChildId(authentication)));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
