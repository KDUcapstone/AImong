package com.aimong.backend.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;

public record MissionQuestionCheckRequest(
        @NotBlank(message = "정답을 입력해 주세요.") String answer
) {
}
