package com.aimong.backend.domain.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record ChildMeResponse(
        UUID childId,
        String nickname,
        String profileImageType,
        int totalXp,
        boolean hasFcmToken,
        Instant lastActiveAt
) {
}
