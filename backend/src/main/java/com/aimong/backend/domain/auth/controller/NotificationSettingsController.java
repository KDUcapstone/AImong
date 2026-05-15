package com.aimong.backend.domain.auth.controller;

import com.aimong.backend.domain.auth.dto.NotificationSettingsRequest;
import com.aimong.backend.domain.auth.dto.NotificationSettingsResponse;
import com.aimong.backend.domain.auth.service.NotificationSettingsService;
import com.aimong.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification/settings")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    @GetMapping
    public ApiResponse<NotificationSettingsResponse> getSettings(Authentication authentication) {
        return ApiResponse.success(notificationSettingsService.getSettings(authentication.getName()));
    }

    @PatchMapping
    public ApiResponse<NotificationSettingsResponse> updateSettings(
            Authentication authentication,
            @RequestBody(required = false) NotificationSettingsRequest request
    ) {
        return ApiResponse.success(notificationSettingsService.updateSettings(authentication.getName(), request));
    }
}
