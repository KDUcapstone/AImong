package com.aimong.backend.domain.gacha.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.dto.GachaPullResponse;
import com.aimong.backend.domain.gacha.dto.GachaExchangeResponse;
import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.GachaPullRepository;
import com.aimong.backend.domain.gacha.repository.FragmentRepository;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.pet.service.PetService;
import com.aimong.backend.infra.fcm.FcmService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GachaPullServiceTest {

    @Mock
    private GachaProbabilityService gachaProbabilityService;

    @Mock
    private GachaPullRepository gachaPullRepository;

    @Mock
    private ChildProfileRepository childProfileRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private FragmentRepository fragmentRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetService petService;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private GachaPullService gachaPullService;

    @Test
    void pullConsumesTicketAndGrantsNewPet() {
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-uid", "parent@example.com"),
                "민지",
                "482917"
        );
        Ticket ticket = Ticket.issue(childProfile.getId(), TicketType.NORMAL);
        Pet grantedPet = Pet.create(childProfile.getId(), "pet_rare_003", PetGrade.RARE);

        when(childProfileRepository.findWithLockById(childProfile.getId())).thenReturn(Optional.of(childProfile));
        when(ticketRepository.findFirstByChildIdAndTicketTypeAndUsedAtIsNullOrderByCreatedAtAsc(
                childProfile.getId(),
                TicketType.NORMAL
        )).thenReturn(Optional.of(ticket));
        when(gachaProbabilityService.draw(TicketType.NORMAL, 1, 0))
                .thenReturn(new GachaProbabilityService.DrawResult("pet_rare_003", PetGrade.RARE, 0.0d));
        when(gachaProbabilityService.petNameOf("pet_rare_003")).thenReturn("번개몽");
        when(petRepository.existsByChildIdAndPetType(childProfile.getId(), "pet_rare_003")).thenReturn(false);
        when(petService.grantPet(childProfile.getId(), "pet_rare_003", PetGrade.RARE)).thenReturn(grantedPet);
        when(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(eq(childProfile.getId()), any(TicketType.class)))
                .thenReturn(0L);

        GachaPullResponse response = gachaPullService.pull(childProfile.getId(), TicketType.NORMAL);

        assertThat(ticket.getUsedAt()).isNotNull();
        assertThat(childProfile.getGachaPullCount()).isEqualTo(1);
        assertThat(childProfile.getSrMissCount()).isEqualTo(1);
        assertThat(response.result().petId()).isEqualTo(grantedPet.getId());
        assertThat(response.result().petType()).isEqualTo("pet_rare_003");
        assertThat(response.result().petName()).isEqualTo("번개몽");
        assertThat(response.result().grade()).isEqualTo("RARE");
        assertThat(response.result().isNew()).isTrue();
        verify(gachaPullRepository).save(any());
    }

    @Test
    void pullSendsParentFcmWhenGachaLevelBoundaryIsCrossed() {
        ParentAccount parentAccount = ParentAccount.create("firebase-uid", "parent@example.com");
        parentAccount.updateFcmToken("parent-fcm-token");
        ChildProfile childProfile = ChildProfile.create(parentAccount, "민지", "482917");
        ReflectionTestUtils.setField(childProfile, "gachaPullCount", 19);
        Ticket ticket = Ticket.issue(childProfile.getId(), TicketType.NORMAL);
        Pet grantedPet = Pet.create(childProfile.getId(), "pet_rare_003", PetGrade.RARE);

        when(childProfileRepository.findWithLockById(childProfile.getId())).thenReturn(Optional.of(childProfile));
        when(ticketRepository.findFirstByChildIdAndTicketTypeAndUsedAtIsNullOrderByCreatedAtAsc(
                childProfile.getId(),
                TicketType.NORMAL
        )).thenReturn(Optional.of(ticket));
        when(gachaProbabilityService.draw(TicketType.NORMAL, 20, 0))
                .thenReturn(new GachaProbabilityService.DrawResult("pet_rare_003", PetGrade.RARE, 0.0d));
        when(gachaProbabilityService.petNameOf("pet_rare_003")).thenReturn("번개몽");
        when(petRepository.existsByChildIdAndPetType(childProfile.getId(), "pet_rare_003")).thenReturn(false);
        when(petService.grantPet(childProfile.getId(), "pet_rare_003", PetGrade.RARE)).thenReturn(grantedPet);
        when(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(eq(childProfile.getId()), any(TicketType.class)))
                .thenReturn(0L);

        GachaPullResponse response = gachaPullService.pull(childProfile.getId(), TicketType.NORMAL);

        assertThat(response.levelUp()).isTrue();
        assertThat(childProfile.getShieldCount()).isEqualTo(1);
        verify(fcmService).sendGachaLevelUpToParent("parent-fcm-token", 20);
    }

    @Test
    void exchangeSpendsFragmentsAndGrantsSelectedPet() {
        ParentAccount parentAccount = ParentAccount.create("firebase-uid", "parent@example.com");
        ChildProfile childProfile = ChildProfile.create(parentAccount, "민지", "482917");
        Fragment fragment = Fragment.create(childProfile.getId(), PetGrade.NORMAL);
        fragment.add(10);
        Pet grantedPet = Pet.create(childProfile.getId(), "pet_normal_005", PetGrade.NORMAL);

        when(childProfileRepository.findWithLockById(childProfile.getId())).thenReturn(Optional.of(childProfile));
        when(gachaProbabilityService.isValidPetTypeForGrade(PetGrade.NORMAL, "pet_normal_005")).thenReturn(true);
        when(petRepository.existsByChildIdAndPetType(childProfile.getId(), "pet_normal_005")).thenReturn(false);
        when(fragmentRepository.findWithLockByChildIdAndGrade(childProfile.getId(), PetGrade.NORMAL))
                .thenReturn(Optional.of(fragment));
        when(petService.grantPet(childProfile.getId(), "pet_normal_005", PetGrade.NORMAL)).thenReturn(grantedPet);

        GachaExchangeResponse response = gachaPullService.exchange(childProfile.getId(), PetGrade.NORMAL, "pet_normal_005");

        assertThat(fragment.getCount()).isZero();
        assertThat(response.petId()).isEqualTo(grantedPet.getId());
        assertThat(response.petType()).isEqualTo("pet_normal_005");
        assertThat(response.grade()).isEqualTo("NORMAL");
        assertThat(response.stage()).isEqualTo("EGG");
    }
}
