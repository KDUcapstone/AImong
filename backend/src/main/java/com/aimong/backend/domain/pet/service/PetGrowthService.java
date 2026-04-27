package com.aimong.backend.domain.pet.service;

import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.pet.entity.CrownType;
import com.aimong.backend.domain.pet.entity.EquippedPet;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.entity.PetStage;
import com.aimong.backend.domain.pet.repository.EquippedPetRepository;
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

    private final EquippedPetRepository equippedPetRepository;
    private final PetRepository petRepository;

    @Transactional(readOnly = true)
    public String findEquippedPetGrade(UUID childId) {
        return equippedPetRepository.findByChildId(childId)
                .flatMap(equippedPet -> petRepository.findByIdAndChildId(equippedPet.getPetId(), childId))
                .map(pet -> pet.getGrade().name())
                .orElse(null);
    }

    @Transactional
    public PetGrowthResult applyMissionReward(UUID childId, int petXpAmount, Ticket ticket) {
        EquippedPet equippedPet = equippedPetRepository.findWithLockByChildId(childId).orElse(null);
        if (equippedPet == null) {
            return PetGrowthResult.none();
        }

        Pet pet = petRepository.findWithLockByIdAndChildId(equippedPet.getPetId(), childId)
                .orElseThrow(() -> new AimongException(ErrorCode.INTERNAL_SERVER_ERROR));

        PetStage previousStage = pet.getStage();
        boolean evolved = pet.addXp(petXpAmount);
        boolean crownUnlocked = false;
        List<PetReward> rewards = new ArrayList<>();

        if (!pet.isCrownUnlocked() && pet.getStage() == PetStage.AIMONG) {
            crownUnlocked = true;
            CrownType crownType = crownTypeOf(pet.getGrade());
            pet.unlockCrown(crownType);
            rewards.addAll(applyAimongRewards(pet.getGrade(), ticket));
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

    private List<PetReward> applyAimongRewards(PetGrade grade, Ticket ticket) {
        List<PetReward> rewards = new ArrayList<>();
        switch (grade) {
            case NORMAL -> {
                ticket.addRare(1);
                rewards.add(new PetReward("TICKET", "RARE", 1, "AIMONG_REWARD_NORMAL"));
            }
            case RARE -> {
                ticket.addEpic(1);
                rewards.add(new PetReward("TICKET", "EPIC", 1, "AIMONG_REWARD_RARE"));
            }
            case EPIC -> {
                ticket.addEpic(2);
                rewards.add(new PetReward("TICKET", "EPIC", 2, "AIMONG_REWARD_EPIC"));
            }
            case LEGEND -> {
                ticket.addEpic(3);
                rewards.add(new PetReward("TICKET", "EPIC", 3, "AIMONG_REWARD_LEGEND"));
            }
        }
        return rewards;
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
