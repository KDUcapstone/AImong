package com.aimong.backend.domain.mission.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SubmitRequest(
        @JsonAlias("attemptId")
        UUID quizAttemptId,
        @NotNull(message = "답안 10개를 모두 제출해주세요")
        @Size(min = 10, max = 10, message = "답안 10개를 모두 제출해주세요")
        List<@Valid AnswerRequest> answers
) {
    public record AnswerRequest(
            @NotBlank String questionId,
            @JsonAlias("selected")
            @NotBlank String answer
    ) {
    }
}
