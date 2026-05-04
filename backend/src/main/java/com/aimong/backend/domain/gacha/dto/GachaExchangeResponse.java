package com.aimong.backend.domain.gacha.dto;

import java.util.UUID;

public record GachaExchangeResponse(
        UUID petId,
        String petType,
        String grade,
        String stage
) {
}
