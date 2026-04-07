package com.aimong.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChildLoginRequest(
        @NotBlank(message = "code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "code must be 6 digits")
        String code
) {
}
