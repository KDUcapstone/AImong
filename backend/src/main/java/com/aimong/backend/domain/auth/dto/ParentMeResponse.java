package com.aimong.backend.domain.auth.dto;

import java.time.Instant;

public record ParentMeResponse(
        String parentId,
        String email,
        boolean hasFcmToken,
        long childrenCount,
        Instant createdAt
) {
}
