package com.aimong.backend.domain.quest.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimRequest(
        @NotBlank(message = "퀘스트 타입을 입력해 주세요")
        String questType,

        @NotBlank(message = "기간을 입력해 주세요")
        String period
) {
}
