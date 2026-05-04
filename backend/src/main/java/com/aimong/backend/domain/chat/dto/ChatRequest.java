package com.aimong.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "메시지를 입력해주세요")
        @Size(max = 200, message = "메시지는 200자 이하로 입력해주세요")
        String message,

        @NotNull(message = "마스킹 여부를 입력해주세요")
        Boolean masked
) {
}
