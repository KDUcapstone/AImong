package com.aimong.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChildLoginRequest(
        @NotBlank(message = "코드를 입력해주세요")
        @Pattern(regexp = "^[0-9]{6}$", message = "올바른 형식의 코드를 입력해주세요")
        String code
) {
}
