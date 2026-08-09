package com.aimong.backend.domain.pet.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EquipPetRequest(
        @NotNull(message = "장착할 펫을 선택해주세요")
        UUID petId
) {
}
