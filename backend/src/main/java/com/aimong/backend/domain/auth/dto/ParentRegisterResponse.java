package com.aimong.backend.domain.auth.dto;

import java.util.UUID;

public record ParentRegisterResponse(
        UUID childId,
        String nickname,
        String code,
        int starterTickets
) {
}
