package com.aimong.backend.domain.gacha.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DevGachaGrantRequest(
        @Min(0) @Max(100) int normalTickets,
        @Min(0) @Max(100) int rareTickets,
        @Min(0) @Max(100) int epicTickets,
        @Min(0) @Max(1000) int normalFragments,
        @Min(0) @Max(1000) int rareFragments,
        @Min(0) @Max(1000) int epicFragments,
        @Min(0) @Max(1000) int legendFragments
) {
}
