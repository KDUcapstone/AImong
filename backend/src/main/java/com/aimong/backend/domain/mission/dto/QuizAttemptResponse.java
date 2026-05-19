package com.aimong.backend.domain.mission.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizAttemptResponse(
        UUID attemptId,
        String setId,
        UUID missionId,
        int starLevel,
        String status,
        boolean isReview,
        Instant expiresAt,
        long remainingSeconds,
        List<String> answeredQuestionIds,
        int remainingLives,
        int wrongCountInSession,
        int reviveCount,
        boolean canRevive,
        int questionCount
) {
}
