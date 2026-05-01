package com.aimong.backend.domain.pet.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.repository.PetRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final ChildProfileRepository childProfileRepository;

    public Pet grantPet(UUID childId, String petType, PetGrade grade) {
        return petRepository.save(Pet.create(childId, petType, grade));
    }

    public void equipPet(UUID childId, UUID petId) {
        Pet pet = petRepository.findByIdAndChildId(petId, childId)
                .orElseThrow();
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow();
        childProfile.equipPet(pet.getId());
    }
}
