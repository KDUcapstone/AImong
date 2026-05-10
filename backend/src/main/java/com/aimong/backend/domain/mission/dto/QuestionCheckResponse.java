package com.aimong.backend.domain.mission.dto;

import java.util.UUID;

public record QuestionCheckResponse(
        UUID questionId,
        boolean isCorrect,
        String correctAnswer,
        String explanation
) {
    public QuestionCheckResponse(UUID questionId, boolean isCorrect, String explanation) {
        this(questionId, isCorrect, null, explanation);
    }
}
