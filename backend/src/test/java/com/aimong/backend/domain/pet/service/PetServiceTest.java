package com.aimong.backend.domain.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.pet.dto.EquipPetResponse;
import com.aimong.backend.domain.pet.dto.PetListResponse;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.entity.PetStage;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private ChildProfileRepository childProfileRepository;

    @Mock
    private ChildActivityService childActivityService;

    @Mock
    private StreakRecordRepository streakRecordRepository;

    @InjectMocks
    private PetService petService;

    @Test
    void getPetsReturnsEquippedPetAndOwnedPets() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "민지",
                "482917"
        );
        Pet equipped = Pet.create(childProfile.getId(), "pet_normal_001", PetGrade.NORMAL);
        Pet other = Pet.create(childProfile.getId(), "pet_rare_003", PetGrade.RARE);
        childProfile.equipPet(equipped.getId());

        when(childProfileRepository.findById(childProfile.getId())).thenReturn(Optional.of(childProfile));
        when(petRepository.findByChildIdOrderByObtainedAtDesc(childProfile.getId()))
                .thenReturn(List.of(other, equipped));

        PetListResponse response = petService.getPets(childProfile.getId());

        assertThat(response.equippedPet().id()).isEqualTo(equipped.getId());
        assertThat(response.pets()).hasSize(2);
        assertThat(response.totalPetCount()).isEqualTo(2);
        verify(childActivityService).touchLastActiveAt(childProfile.getId());
    }

    @Test
    void equipPetUpdatesChildProfileEquippedPet() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "민지",
                "482917"
        );
        Pet pet = Pet.create(childProfile.getId(), "pet_rare_003", PetGrade.RARE);

        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        when(childProfileRepository.findWithLockById(childProfile.getId())).thenReturn(Optional.of(childProfile));

        EquipPetResponse response = petService.equipPet(childProfile.getId(), pet.getId());

        assertThat(response.equippedPetId()).isEqualTo(pet.getId());
        assertThat(response.petType()).isEqualTo("pet_rare_003");
        assertThat(response.grade()).isEqualTo("RARE");
        assertThat(response.stage()).isEqualTo("EGG");
        assertThat(childProfile.getEquippedPetId()).isEqualTo(pet.getId());
        verify(childActivityService).touchLastActiveAt(childProfile.getId());
    }

    @Test
    void equipPetRejectsPetOwnedByOtherChild() {
        UUID childId = UUID.randomUUID();
        Pet otherChildPet = Pet.create(UUID.randomUUID(), "pet_rare_003", PetGrade.RARE);

        when(petRepository.findById(otherChildPet.getId())).thenReturn(Optional.of(otherChildPet));

        assertThatThrownBy(() -> petService.equipPet(childId, otherChildPet.getId()))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void equipPetRejectsMissingPet() {
        UUID childId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.equipPet(childId, petId))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PET_NOT_FOUND);
    }

    @Test
    void petEvolutionUsesApiGradeThresholdsAndResetsXp() {
        Pet normalPet = Pet.create(UUID.randomUUID(), "pet_normal_001", PetGrade.NORMAL);

        boolean evolvedToGrowth = normalPet.addXp(10);
        boolean evolvedToAimong = normalPet.addXp(30);

        assertThat(evolvedToGrowth).isTrue();
        assertThat(evolvedToAimong).isTrue();
        assertThat(normalPet.getStage()).isEqualTo(PetStage.AIMONG);
        assertThat(normalPet.getXp()).isZero();
    }

    @Test
    void petEvolutionWaitsForGradeSpecificThreshold() {
        Pet rarePet = Pet.create(UUID.randomUUID(), "pet_rare_003", PetGrade.RARE);

        boolean notYet = rarePet.addXp(11);
        boolean evolved = rarePet.addXp(1);

        assertThat(notYet).isFalse();
        assertThat(rarePet.getStage()).isEqualTo(PetStage.GROWTH);
        assertThat(evolved).isTrue();
        assertThat(rarePet.getXp()).isZero();
    }
}
