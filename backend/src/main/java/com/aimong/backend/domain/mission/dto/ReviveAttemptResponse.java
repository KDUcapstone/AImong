package com.aimong.backend.domain.mission.dto;

import java.util.UUID;

public record ReviveAttemptResponse(
        UUID attemptId,
        int remainingLives,
        int reviveCount,
        int reviveCost,
        int gearBalance
) {
}
