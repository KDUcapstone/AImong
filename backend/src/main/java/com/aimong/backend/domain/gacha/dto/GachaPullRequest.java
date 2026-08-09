package com.aimong.backend.domain.gacha.dto;

import com.aimong.backend.domain.gacha.entity.TicketType;
import jakarta.validation.constraints.NotNull;

public record GachaPullRequest(
        @NotNull(message = "티켓 종류를 선택해주세요")
        TicketType ticketType
) {
}
