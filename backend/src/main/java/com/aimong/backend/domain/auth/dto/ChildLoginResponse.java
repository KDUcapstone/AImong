package com.aimong.backend.domain.auth.dto;

import java.util.UUID;

public record ChildLoginResponse(
        UUID childId,
        String nickname,
        String sessionToken,
        String profileImageType,
        int totalXp,
        int level
) {
}
