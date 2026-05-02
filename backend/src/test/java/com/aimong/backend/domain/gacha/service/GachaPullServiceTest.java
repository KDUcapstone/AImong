package com.aimong.backend.domain.gacha.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.gacha.repository.GachaPullRepository;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.pet.service.PetService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GachaPullServiceTest {

    @Mock
    private GachaProbabilityService gachaProbabilityService;

    @Mock
    private FragmentService fragmentService;

    @Mock
    private GachaPullRepository gachaPullRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetService petService;

    @InjectMocks
    private GachaPullService gachaPullService;

    @Test
    void initializeStarterOnboardingCreatesStarterPetAndEquip() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "민준",
                "482917"
        );
        Pet grantedPet = Pet.create(childProfile.getId(), "RABBIT", PetGrade.NORMAL);

        when(petRepository.findByChildId(childProfile.getId())).thenReturn(List.of());
        when(gachaProbabilityService.drawStarterPet())
                .thenReturn(new GachaProbabilityService.StarterPetResult("RABBIT", PetGrade.NORMAL));
        when(petService.grantPet(childProfile.getId(), "RABBIT", PetGrade.NORMAL)).thenReturn(grantedPet);

        gachaPullService.initializeStarterOnboarding(childProfile);

        verify(fragmentService).initializeInventory(childProfile.getId());
        verify(petService).grantPet(childProfile.getId(), "RABBIT", PetGrade.NORMAL);
        verify(petService).equipPet(childProfile.getId(), grantedPet.getId());
        verify(gachaPullRepository).save(any());
        assertThat(childProfile.getGachaPullCount()).isEqualTo(1);
        assertThat(childProfile.getSrMissCount()).isEqualTo(1);
    }
}
