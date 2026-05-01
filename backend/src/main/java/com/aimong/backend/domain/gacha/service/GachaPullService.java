package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.gacha.entity.GachaPull;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.GachaPullRepository;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.pet.service.PetService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GachaPullService {

    private final GachaProbabilityService gachaProbabilityService;
    private final FragmentService fragmentService;
    private final GachaPullRepository gachaPullRepository;
    private final PetRepository petRepository;
    private final PetService petService;

    @Transactional
    public void initializeStarterOnboarding(ChildProfile childProfile) {
        UUID childId = childProfile.getId();
        fragmentService.initializeInventory(childId);

        if (!petRepository.findByChildId(childId).isEmpty()) {
            return;
        }

        GachaProbabilityService.StarterPetResult starterPet = gachaProbabilityService.drawStarterPet();
        Pet grantedPet = petService.grantPet(childId, starterPet.getPetType(), starterPet.getGrade());
        petService.equipPet(childId, grantedPet.getId());

        gachaPullRepository.save(GachaPull.create(
                childId,
                TicketType.NORMAL,
                starterPet.getPetType(),
                starterPet.getGrade(),
                true,
                grantedPet.getId(),
                0,
                childProfile.getSrMissCount()
        ));
        childProfile.recordGachaPull(starterPet.getGrade());
    }
}
