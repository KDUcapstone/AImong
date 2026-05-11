package com.aimong.backend.domain.home.dto;

import java.time.Instant;

public record EnergyResponse(
        int energy,
        int maxEnergy,
        Instant nextEnergyRecoverAt,
        Instant fullRecoverAt,
        int recoverIntervalMinutes,
        int missionStartCost
) {
}
