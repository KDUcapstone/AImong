package com.aimong.backend.domain.home.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EnergyAddRequest(
        @NotNull(message = "추가 에너지를 입력해 주세요.")
        @Min(value = 1, message = "추가 에너지는 1~20 사이여야 합니다.")
        @Max(value = 20, message = "추가 에너지는 1~20 사이여야 합니다.")
        Integer amount
) {
}
