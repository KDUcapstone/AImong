package com.aimong.backend.domain.pet.dto;

import com.aimong.backend.domain.pet.entity.Pet;
import java.util.UUID;

public record EquipPetResponse(
        UUID equippedPetId,
        String petType,
        String grade,
        String stage
) {

    public static EquipPetResponse from(Pet pet) {
        return new EquipPetResponse(
                pet.getId(),
                pet.getPetType(),
                pet.getGrade().name(),
                pet.getStage().name()
        );
    }
}
