package com.aimong.backend.domain.home.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DevEnergyAddRequest(
        @NotNull(message = "異붽? ?먮꼫吏瑜??낅젰??二쇱꽭??")
        @Min(value = 1, message = "異붽? ?먮꼫吏??1~20 ?ъ씠?ъ빞 ?⑸땲??")
        @Max(value = 20, message = "異붽? ?먮꼫吏??1~20 ?ъ씠?ъ빞 ?⑸땲??")
        Integer amount
) {
}
