package com.aimong.backend.domain.reward.dto;

public record WalletResponse(
        int gear,
        CostsResponse costs
) {
    public record CostsResponse(
            int heartRevive,
            int streakShield
    ) {
    }
}
