package com.aimong.backend.domain.gacha.dto;

import java.util.List;

public record FragmentListResponse(
        List<FragmentSummary> fragments
) {
    public record FragmentSummary(
            String grade,
            int count,
            int exchangeThreshold
    ) {
    }
}
