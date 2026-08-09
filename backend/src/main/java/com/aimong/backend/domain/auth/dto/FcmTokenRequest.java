package com.aimong.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰을 입력해주세요")
        String fcmToken
) {
}
