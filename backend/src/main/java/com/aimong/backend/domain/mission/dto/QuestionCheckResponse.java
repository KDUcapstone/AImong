package com.aimong.backend.domain.mission.dto;

import java.util.UUID;

public record QuestionCheckResponse(
        UUID questionId,
        boolean isCorrect,
        String correctAnswer,
        String explanation,
        int remainingLives,
        boolean canRevive,
        int reviveCost,
        int gearBalance,
        java.util.List<String> nextActions
) {
    public QuestionCheckResponse(UUID questionId, boolean isCorrect, String explanation) {
        this(questionId, isCorrect, null, explanation, 3, false, 10, 0, java.util.List.of());
    }

    public QuestionCheckResponse(UUID questionId, boolean isCorrect, String correctAnswer, String explanation) {
        this(questionId, isCorrect, correctAnswer, explanation, 3, false, 10, 0, java.util.List.of());
    }
}
