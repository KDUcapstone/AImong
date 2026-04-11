package com.aimong.backend.domain.mission.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SubmitRequest(
        @NotNull UUID quizAttemptId,
        @NotEmpty List<@Valid AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotNull UUID questionId,
            @NotBlank String selected
    ) {
    }
}
