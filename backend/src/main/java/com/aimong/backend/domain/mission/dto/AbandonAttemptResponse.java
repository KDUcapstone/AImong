package com.aimong.backend.domain.mission.dto;

import java.util.UUID;

public record AbandonAttemptResponse(
        boolean abandoned,
        UUID attemptId,
        String status,
        boolean energyRefunded
) {
}
