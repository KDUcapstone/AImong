package com.aimong.backend.domain.streak.dto;

import java.util.UUID;

public record PartnerConnectResponse(
        PartnerResponse partner
) {

    public record PartnerResponse(
            UUID childId,
            String nickname
    ) {
    }
}
