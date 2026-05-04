package com.aimong.backend.domain.gacha.dto;

public record DevGachaGrantResponse(
        GachaPullResponse.RemainingTickets remainingTickets,
        FragmentListResponse fragments
) {
}
