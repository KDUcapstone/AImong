package com.aimong.backend.domain.mission.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MissionStatusResponse(
        UUID missionId,
        String missionCode,
        String title,
        boolean isUnlocked,
        boolean canStartMission,
        int energyRequired,
        EnergyStatus energy,
        List<StarLevelStatus> starLevels,
        InProgressAttempt inProgressAttempt
) {
    public record EnergyStatus(
            int current,
            int required,
            int maxEnergy,
            Instant nextEnergyRecoverAt
    ) {
    }

    public record StarLevelStatus(
            int starLevel,
            String label,
            long totalSetCount,
            long completedSetCount,
            boolean isPlayable,
            boolean isReviewable
    ) {
    }

    public record InProgressAttempt(
            UUID attemptId,
            String setId,
            int starLevel,
            Instant expiresAt
    ) {
    }
}
