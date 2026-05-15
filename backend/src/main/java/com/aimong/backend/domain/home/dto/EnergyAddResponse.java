package com.aimong.backend.domain.home.dto;

import java.time.Instant;

public record EnergyAddResponse(
        int energy,
        int maxEnergy,
        int addedEnergy,
        Instant nextEnergyRecoverAt,
        Instant fullRecoverAt
) {
}
