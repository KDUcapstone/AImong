package com.aimong.backend.domain.pet.service;

import com.aimong.backend.domain.pet.entity.EquippedPet;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.repository.EquippedPetRepository;
import com.aimong.backend.domain.pet.repository.PetRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final EquippedPetRepository equippedPetRepository;

    public Pet grantPet(UUID childId, String petType, PetGrade grade) {
        return petRepository.save(Pet.create(childId, petType, grade));
    }

    public void equipPet(UUID childId, UUID petId) {
        equippedPetRepository.findByChildId(childId)
                .ifPresentOrElse(
                        equippedPet -> equippedPet.changePet(petId),
                        () -> equippedPetRepository.save(EquippedPet.create(childId, petId))
                );
    }
}
