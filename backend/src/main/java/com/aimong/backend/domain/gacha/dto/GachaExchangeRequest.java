package com.aimong.backend.domain.gacha.dto;

import com.aimong.backend.domain.pet.entity.PetGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GachaExchangeRequest(
        @NotNull(message = "교환할 펫 등급을 선택해주세요")
        PetGrade grade,
        @NotBlank(message = "교환할 펫 종류를 선택해주세요")
        String petType
) {
}
