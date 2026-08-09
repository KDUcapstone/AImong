package com.aimong.backend.domain.auth.dto;

import java.util.List;
import java.util.UUID;

public record ParentChildrenResponse(
        List<ChildSummary> children
) {
    public record ChildSummary(
            UUID childId,
            String nickname,
            String code,
            String profileImageType,
            int totalXp
    ) {
    }
}
