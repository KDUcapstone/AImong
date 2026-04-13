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
        Integer previousLevel,
        Integer level,
        Boolean levelUp,
        Integer nextLevelTargetXp,
        Integer equippedPetXp,
        String petStage,
        boolean petEvolved,
        boolean crownUnlocked,
        String crownType,
        int streakDays,
        List<RewardResponse> levelRewards,
        List<RewardResponse> rewards,
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
}
