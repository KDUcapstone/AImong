package com.aimong.backend.domain.pet.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.pet.dto.EquipPetResponse;
import com.aimong.backend.domain.pet.dto.PetListResponse;
import com.aimong.backend.domain.pet.dto.PetSummaryResponse;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.entity.PetMood;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final StreakRecordRepository streakRecordRepository;

    @Transactional
    public Pet grantPet(UUID childId, String petType, PetGrade grade) {
        return petRepository.save(Pet.create(childId, petType, grade));
    }

    @Transactional
    public PetListResponse getPets(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        List<Pet> pets = petRepository.findByChildIdOrderByObtainedAtDesc(childId);
        updateMoodsFromLastMission(childId, pets);

        PetSummaryResponse equippedPet = pets.stream()
                .filter(pet -> Objects.equals(pet.getId(), childProfile.getEquippedPetId()))
                .findFirst()
                .map(PetSummaryResponse::from)
                .orElse(null);

        List<PetSummaryResponse> petResponses = pets.stream()
                .map(PetSummaryResponse::from)
                .toList();

        return new PetListResponse(equippedPet, petResponses, pets.size());
    }

    @Transactional
    public EquipPetResponse equipPet(UUID childId, UUID petId) {
        childActivityService.touchLastActiveAt(childId);
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new AimongException(ErrorCode.PET_NOT_FOUND));
        if (!pet.getChildId().equals(childId)) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }

        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        childProfile.equipPet(pet.getId());
        return EquipPetResponse.from(pet);
    }

    private void updateMoodsFromLastMission(UUID childId, List<Pet> pets) {
        PetMood mood = streakRecordRepository.findById(childId)
                .map(record -> moodOf(record.getLastCompletedDate(), KstDateUtils.today()))
                .orElse(PetMood.IDLE);
        pets.forEach(pet -> pet.updateMood(mood));
    }

    private PetMood moodOf(LocalDate lastCompletedDate, LocalDate today) {
        if (lastCompletedDate == null) {
            return PetMood.IDLE;
        }
        if (lastCompletedDate.equals(today)) {
            return PetMood.HAPPY;
        }
        if (lastCompletedDate.equals(today.minusDays(1))) {
            return PetMood.SAD_LIGHT;
        }
        return PetMood.SAD_DEEP;
    }
}
