package com.aimong.backend.domain.mission.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MissionQuestionsResponse(
        String setId,
        UUID missionId,
        String missionCode,
        int starLevel,
        int variantNo,
        int stage,
        String label,
        String title,
        String description,
        boolean isReview,
        int energyCost,
        Integer energyBefore,
        Integer energyAfter,
        @JsonProperty("attemptId")
        UUID quizAttemptId,
        int questionCount,
        Instant expiresAt,
        List<QuestionResponse> questions
) {
    public static String labelForStar(int starLevel) {
        return MissionListResponse.labelForStar(starLevel);
    }
}
