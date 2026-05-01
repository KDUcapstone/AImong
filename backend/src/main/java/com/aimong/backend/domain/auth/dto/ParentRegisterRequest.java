package com.aimong.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentRegisterRequest(
        @NotBlank(message = "닉네임을 입력해주세요")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
        String nickname
) {
}
