package com.aimong.backend.domain.pet.dto;

import com.aimong.backend.domain.pet.entity.Pet;
import java.time.Instant;
import java.util.UUID;

public record PetSummaryResponse(
        UUID id,
        String petType,
        String grade,
        int xp,
        String stage,
        String mood,
        boolean crownUnlocked,
        String crownType,
        Instant obtainedAt
) {

    public static PetSummaryResponse from(Pet pet) {
        return new PetSummaryResponse(
                pet.getId(),
                pet.getPetType(),
                pet.getGrade().name(),
                pet.getXp(),
                pet.getStage().name(),
                pet.getMood().name(),
                pet.isCrownUnlocked(),
                pet.getCrownType() == null ? null : pet.getCrownType().name(),
                pet.getObtainedAt()
        );
    }
}
