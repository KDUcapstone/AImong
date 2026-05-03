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
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.GachaPullRepository;
import com.aimong.backend.domain.gacha.repository.FragmentRepository;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.pet.service.PetService;
import java.util.Optional;
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
        Pet grantedPet = Pet.create(childProfile.getId(), "FOX", PetGrade.RARE);

        when(childProfileRepository.findWithLockById(childProfile.getId())).thenReturn(Optional.of(childProfile));
        when(ticketRepository.findFirstByChildIdAndTicketTypeAndUsedAtIsNullOrderByCreatedAtAsc(
                childProfile.getId(),
                TicketType.NORMAL
        )).thenReturn(Optional.of(ticket));
        when(gachaProbabilityService.draw(TicketType.NORMAL, 1, 0))
                .thenReturn(new GachaProbabilityService.DrawResult("FOX", PetGrade.RARE, 0.0d));
        when(petRepository.existsByChildIdAndPetType(childProfile.getId(), "FOX")).thenReturn(false);
        when(petService.grantPet(childProfile.getId(), "FOX", PetGrade.RARE)).thenReturn(grantedPet);
        when(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(eq(childProfile.getId()), any(TicketType.class)))
                .thenReturn(0L);

        GachaPullResponse response = gachaPullService.pull(childProfile.getId(), TicketType.NORMAL);

        assertThat(ticket.getUsedAt()).isNotNull();
        assertThat(childProfile.getGachaPullCount()).isEqualTo(1);
        assertThat(childProfile.getSrMissCount()).isEqualTo(1);
        assertThat(response.result().petId()).isEqualTo(grantedPet.getId());
        assertThat(response.result().grade()).isEqualTo("RARE");
        assertThat(response.result().isNew()).isTrue();
        verify(gachaPullRepository).save(any());
    }
}
