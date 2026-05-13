package com.aimong.backend.domain.mission.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MissionSetReportResponse(
        UUID attemptId,
        String setId,
        UUID missionId,
        String missionCode,
        int starLevel,
        int variantNo,
        int score,
        int correctCount,
        int wrongCount,
        int questionCount,
        boolean isPassed,
        boolean isPerfect,
        boolean isReview,
        Instant submittedAt,
        RewardsResponse rewards,
        List<ResultResponse> results
) {
    public record RewardsResponse(
            int gear,
            int exp,
            List<FragmentResponse> fragments
    ) {
    }

    public record FragmentResponse(
            String grade,
            int count
    ) {
    }

    public record ResultResponse(
            UUID questionId,
            int questionNo,
            boolean isCorrect,
            String correctAnswer,
            String submittedAnswer,
            String explanation
    ) {
    }

}
