package com.aimong.backend.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record QuestionCheckRequest(
        @NotNull(message = "채점할 문제 세션을 확인해주세요") UUID quizAttemptId,
        @NotBlank(message = "채점할 답안을 확인해주세요") String selected
) {
}
