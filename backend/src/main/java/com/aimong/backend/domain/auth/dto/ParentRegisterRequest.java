package com.aimong.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentRegisterRequest(
        @NotBlank(message = "nickname is required")
        @Size(max = 20, message = "nickname must be 20 characters or fewer")
        String nickname
) {
}
