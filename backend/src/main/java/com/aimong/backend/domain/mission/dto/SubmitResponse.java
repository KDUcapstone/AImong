package com.aimong.backend.domain.mission.dto;

import java.util.List;
import java.util.UUID;

public record SubmitResponse(
        String mode,
        boolean progressApplied,
        String attemptState,
        UUID attemptId,
        int score,
        int total,
        int correctCount,
        int questionCount,
        boolean isFirstClear,
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
        RewardsResponse rewards,
        RemainingTicketsResponse remainingTickets,
        String profileImageType,
        boolean profileImageUnlocked,
        boolean isReview,
        List<ResultResponse> results,
        String setId,
        String missionId,
        Integer starLevel,
        Integer variantNo,
        long completedSetCount,
        long starLevelCompletedSetCount,
        List<String> nextUnlockedSetIds,
        int todaySetCount
) {
    public record RewardsResponse(
            int coin,
            int exp,
            List<FragmentResponse> fragments
    ) {
    }

    public record FragmentResponse(
            String grade,
            int count
    ) {
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
