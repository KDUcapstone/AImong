package com.aimong.backend.domain.mission.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MissionQuestionsResponse(
        UUID missionId,
        String missionTitle,
        boolean isReview,
        UUID quizAttemptId,
        int questionCount,
        Instant expiresAt,
        List<QuestionResponse> questions
) {
}
