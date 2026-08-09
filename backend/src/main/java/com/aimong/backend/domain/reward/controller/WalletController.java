package com.aimong.backend.domain.reward.controller;

import com.aimong.backend.domain.reward.dto.WalletResponse;
import com.aimong.backend.domain.reward.service.WalletService;
import com.aimong.backend.global.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletResponse> getWallet(Authentication authentication) {
        return ApiResponse.success(walletService.getWallet(UUID.fromString(authentication.getName())));
    }
}
