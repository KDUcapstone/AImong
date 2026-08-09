package com.aimong.backend.domain.streak.dto;

import jakarta.validation.constraints.Min;

public record ShieldPurchaseRequest(
        @Min(1) int count
) {
}
