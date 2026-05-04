package com.aimong.backend.domain.gacha.dto;

import java.util.UUID;

public record GachaPullResponse(
        Result result,
        int srMissCount,
        double srBonus,
        boolean levelUp,
        RemainingTickets remainingTickets
) {
    public record Result(
            UUID petId,
            String petType,
            String petName,
            String grade,
            boolean isNew,
            int fragmentsGot
    ) {
    }

    public record RemainingTickets(
            int normal,
            int rare,
            int epic
    ) {
    }
}
