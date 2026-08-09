package com.aimong.backend.domain.mission.dto;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MissionQuestionsResponse(
        String setId,
        UUID missionId,
        String missionCode,
        int starLevel,
        int variantNo,
        String label,
        boolean isReview,
        int energyCost,
        Integer energyBefore,
        Integer energyAfter,
        @JsonProperty("attemptId")
        UUID quizAttemptId,
        int questionCount,
        List<QuestionResponse> questions
) {
    public static String labelForStar(int starLevel) {
        return MissionListResponse.labelForStar(starLevel);
    }
}
