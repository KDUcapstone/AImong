package com.aimong.backend.domain.mission.dto;

import java.util.List;

public record SubmitResponse(
        String mode,
        boolean progressApplied,
        String attemptState,
        int score,
        int total,
        int wrongCount,
        boolean isPassed,
        boolean isPerfect,
        String equippedPetGrade,
        Integer bonusXp,
        String bonusReason,
        int xpEarned,
        Integer equippedPetXp,
        String petStage,
        boolean petEvolved,
        boolean crownUnlocked,
        String crownType,
        int streakDays,
        int todayMissionCount,
        boolean streakBonusApplied,
        List<RewardResponse> rewards,
        RemainingTicketsResponse remainingTickets,
        String profileImageType,
        boolean profileImageUnlocked,
        boolean isReview,
        List<ResultResponse> results
) {
    public record RewardResponse(
            String type,
            String ticketType,
            Integer count,
            Integer amount,
            String reason
    ) {
    }

    public record ResultResponse(
            String questionId,
            boolean isCorrect,
            String explanation
    ) {
    }

    public record RemainingTicketsResponse(
            int normal,
            int rare,
            int epic
    ) {
    }
}
