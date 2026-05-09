package com.aimong.backend.domain.mission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MissionQuestionsResponse(
        String setId,
        UUID missionId,
        String missionCode,
        int levelNo,
        int stage,
        String difficulty,
        String title,
        String description,
        boolean isReview,
        UUID quizAttemptId,
        int questionCount,
        Instant expiresAt,
        List<QuestionResponse> questions
) {
    public MissionQuestionsResponse(
            UUID missionId,
            String missionTitle,
            boolean isReview,
            UUID quizAttemptId,
            int questionCount,
            Instant expiresAt,
            List<QuestionResponse> questions
    ) {
        this(
                null,
                missionId,
                null,
                1,
                1,
                "LOW",
                missionTitle,
                null,
                isReview,
                quizAttemptId,
                questionCount,
                expiresAt,
                questions
        );
    }

    @JsonProperty("missionTitle")
    public String missionTitle() {
        return title;
    }
}
