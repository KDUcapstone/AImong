package com.aimong.backend.domain.pet.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.pet.entity.CrownType;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.entity.PetStage;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetGrowthService {

    private final ChildProfileRepository childProfileRepository;
    private final PetRepository petRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public String findEquippedPetGrade(UUID childId) {
        return childProfileRepository.findById(childId)
                .map(ChildProfile::getEquippedPetId)
                .flatMap(petId -> petRepository.findByIdAndChildId(petId, childId))
                .map(pet -> pet.getGrade().name())
                .orElse(null);
    }

    @Transactional
    public PetGrowthResult applyMissionReward(UUID childId, int petXpAmount) {
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId).orElse(null);
        if (childProfile == null || childProfile.getEquippedPetId() == null) {
            return PetGrowthResult.none();
        }

        Pet pet = petRepository.findWithLockByIdAndChildId(childProfile.getEquippedPetId(), childId)
                .orElseThrow(() -> new AimongException(ErrorCode.INTERNAL_SERVER_ERROR));

        PetStage previousStage = pet.getStage();
        boolean evolved = pet.addXp(petXpAmount);
        boolean crownUnlocked = false;
        List<PetReward> rewards = new ArrayList<>();

        if (!pet.isCrownUnlocked() && pet.getStage() == PetStage.AIMONG) {
            crownUnlocked = true;
            CrownType crownType = crownTypeOf(pet.getGrade());
            pet.unlockCrown(crownType);
            rewards.addAll(applyAimongRewards(childId, pet.getGrade()));
        }

        return new PetGrowthResult(
                pet.getGrade().name(),
                pet.getXp(),
                pet.getStage().name(),
                evolved || previousStage != pet.getStage(),
                pet.isCrownUnlocked() || crownUnlocked,
                pet.getCrownType() != null ? pet.getCrownType().name() : null,
                rewards
        );
    }

    private List<PetReward> applyAimongRewards(UUID childId, PetGrade grade) {
        List<PetReward> rewards = new ArrayList<>();
        switch (grade) {
            case NORMAL -> {
                grantTickets(childId, TicketType.RARE, 1);
                rewards.add(new PetReward("TICKET", "RARE", 1, "AIMONG_REWARD_NORMAL"));
            }
            case RARE -> {
                grantTickets(childId, TicketType.EPIC, 1);
                rewards.add(new PetReward("TICKET", "EPIC", 1, "AIMONG_REWARD_RARE"));
            }
            case EPIC -> {
                grantTickets(childId, TicketType.EPIC, 2);
                rewards.add(new PetReward("TICKET", "EPIC", 2, "AIMONG_REWARD_EPIC"));
            }
            case LEGEND -> {
                grantTickets(childId, TicketType.EPIC, 3);
                rewards.add(new PetReward("TICKET", "EPIC", 3, "AIMONG_REWARD_LEGEND"));
            }
        }
        return rewards;
    }

    private void grantTickets(UUID childId, TicketType ticketType, int count) {
        ticketRepository.saveAll(java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> Ticket.issue(childId, ticketType))
                .toList());
    }

    private CrownType crownTypeOf(PetGrade grade) {
        return switch (grade) {
            case NORMAL -> CrownType.silver;
            case RARE -> CrownType.gold;
            case EPIC -> CrownType.jewel;
            case LEGEND -> CrownType.shining;
        };
    }

    public record PetGrowthResult(
            String equippedPetGrade,
            Integer equippedPetXp,
            String petStage,
            boolean petEvolved,
            boolean crownUnlocked,
            String crownType,
            List<PetReward> rewards
    ) {
        public static PetGrowthResult none() {
            return new PetGrowthResult(null, null, null, false, false, null, List.of());
        }
    }

    public record PetReward(
            String type,
            String ticketType,
            int count,
            String reason
    ) {
    }
}
