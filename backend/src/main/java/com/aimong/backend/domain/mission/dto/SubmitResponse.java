package com.aimong.backend.domain.mission.dto;

import java.util.List;

public record SubmitResponse(
        int score,
        int total,
        boolean isPerfect,
        int xpEarned,
        Integer equippedPetXp,
        String petStage,
        boolean petEvolved,
        boolean crownUnlocked,
        String crownType,
        int streakDays,
        int todayMissionCount,
        List<RewardResponse> rewards,
        RemainingTicketsResponse remainingTickets,
        String profileImageType,
        boolean profileImageUnlocked,
        boolean isReview,
        List<ResultResponse> results
) {
    public record RewardResponse(
            String type,
            String value
    ) {
    }

    public record RemainingTicketsResponse(
            int normal,
            int rare,
            int epic
    ) {
    }

    public record ResultResponse(
            String questionId,
            boolean isCorrect,
            String explanation
    ) {
    }
}
