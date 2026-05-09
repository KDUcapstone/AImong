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
        List<ResultResponse> results,
        String setId,
        String missionId,
        Integer levelNo,
        String difficulty,
        long completedSetCount,
        long levelCompletedSetCount,
        List<String> nextUnlockedSetIds,
        int todaySetCount
) {
    public SubmitResponse(
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
        this(
                mode,
                progressApplied,
                attemptState,
                score,
                total,
                wrongCount,
                isPassed,
                isPerfect,
                equippedPetGrade,
                bonusXp,
                bonusReason,
                xpEarned,
                equippedPetXp,
                petStage,
                petEvolved,
                crownUnlocked,
                crownType,
                streakDays,
                todayMissionCount,
                streakBonusApplied,
                rewards,
                remainingTickets,
                profileImageType,
                profileImageUnlocked,
                isReview,
                results,
                null,
                null,
                null,
                null,
                0,
                0,
                List.of(),
                todayMissionCount
        );
    }

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
