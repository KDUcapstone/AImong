package com.aimong.backend.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;

public record MissionSetCheckRequest(
        @NotBlank(message = "questionId를 입력해 주세요.") String questionId,
        @NotBlank(message = "정답을 입력해 주세요.") String answer
) {
}
