package com.aimong.backend.domain.streak.dto;

public record ShieldPurchaseResponse(
        int shieldCount,
        int purchasedCount,
        int unitCost,
        int gearBalance
) {
}
