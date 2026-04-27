package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.pet.entity.PetGrade;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class GachaProbabilityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<PetGrade, List<String>> PET_POOL = Map.of(
            PetGrade.NORMAL, List.of("RABBIT", "TURTLE", "HEDGEHOG", "SQUIRREL"),
            PetGrade.RARE, List.of("FOX", "OWL", "CAT"),
            PetGrade.EPIC, List.of("DRAGON", "UNICORN"),
            PetGrade.LEGEND, List.of("PHOENIX")
    );

    public StarterPetResult drawStarterPet() {
        List<String> starterPool = PET_POOL.get(PetGrade.NORMAL);
        String petType = starterPool.get(SECURE_RANDOM.nextInt(starterPool.size()));
        return new StarterPetResult(petType, PetGrade.NORMAL);
    }

    @Getter
    public static class StarterPetResult {

        private final String petType;
        private final PetGrade grade;

        public StarterPetResult(String petType, PetGrade grade) {
            this.petType = petType;
            this.grade = grade;
        }
    }
}
