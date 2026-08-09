package com.aimong.backend.domain.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record ParentChildDetailResponse(
        UUID childId,
        String nickname,
        String code,
        String profileImageType,
        int totalXp,
        boolean hasFcmToken,
        Instant lastActiveAt,
        Instant createdAt
) {
}
