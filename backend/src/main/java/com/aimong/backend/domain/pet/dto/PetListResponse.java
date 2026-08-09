package com.aimong.backend.domain.pet.dto;

import java.util.List;

public record PetListResponse(
        PetSummaryResponse equippedPet,
        List<PetSummaryResponse> pets,
        int totalPetCount
) {
}
