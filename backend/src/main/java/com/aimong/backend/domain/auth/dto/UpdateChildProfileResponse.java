package com.aimong.backend.domain.auth.dto;

import java.util.UUID;

public record UpdateChildProfileResponse(
        UUID childId,
        String nickname,
        String profileImageType
) {
}
