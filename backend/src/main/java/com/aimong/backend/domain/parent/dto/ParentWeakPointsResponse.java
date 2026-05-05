package com.aimong.backend.domain.parent.dto;

import java.util.List;

public record ParentWeakPointsResponse(
        int page,
        int size,
        long totalCount,
        boolean hasNext,
        String analyzedPeriod,
        List<ParentWeakPointResponse> weakPoints
) {
}
