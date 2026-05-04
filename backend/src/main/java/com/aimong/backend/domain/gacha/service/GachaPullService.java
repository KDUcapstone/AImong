package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.gacha.entity.GachaPull;
import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.entity.FragmentId;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.dto.FragmentListResponse;
import com.aimong.backend.domain.gacha.dto.GachaExchangeResponse;
import com.aimong.backend.domain.gacha.dto.GachaPullResponse;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.repository.GachaPullRepository;
import com.aimong.backend.domain.gacha.repository.FragmentRepository;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.pet.service.PetService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GachaPullService {

    private static final Map<PetGrade, Integer> FRAGMENT_REWARDS = Map.of(
            PetGrade.NORMAL, 1,
            PetGrade.RARE, 3,
            PetGrade.EPIC, 8,
            PetGrade.LEGEND, 20
    );

    private static final Map<PetGrade, Integer> EXCHANGE_THRESHOLDS = Map.of(
            PetGrade.NORMAL, 10,
            PetGrade.RARE, 30,
            PetGrade.EPIC, 80,
            PetGrade.LEGEND, 200
    );

    private final GachaProbabilityService gachaProbabilityService;
    private final GachaPullRepository gachaPullRepository;
    private final ChildProfileRepository childProfileRepository;
    private final TicketRepository ticketRepository;
    private final FragmentRepository fragmentRepository;
    private final PetRepository petRepository;
    private final PetService petService;

    @Transactional
    public GachaPullResponse pull(UUID childId, TicketType ticketType) {
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        Ticket ticket = ticketRepository.findFirstByChildIdAndTicketTypeAndUsedAtIsNullOrderByCreatedAtAsc(childId, ticketType)
                .orElseThrow(() -> new AimongException(ErrorCode.BAD_REQUEST, "티켓이 부족해요!"));

        int beforePullCount = childProfile.getGachaPullCount();
        int srMissBefore = childProfile.getSrMissCount();
        GachaProbabilityService.DrawResult drawResult = gachaProbabilityService.draw(
                ticketType,
                beforePullCount + 1,
                srMissBefore
        );

        ticket.markUsed();
        childProfile.recordGachaPull(drawResult.grade());
        boolean levelUp = crossedGachaLevelBoundary(beforePullCount, childProfile.getGachaPullCount());
        if (levelUp) {
            ticketRepository.save(Ticket.issue(childId, TicketType.NORMAL));
            ticketRepository.save(Ticket.issue(childId, TicketType.NORMAL));
            childProfile.addShield(1);
        }

        Pet pet = null;
        int fragmentsGot = 0;
        boolean isNew = !petRepository.existsByChildIdAndPetType(childId, drawResult.petType());
        if (isNew) {
            pet = petService.grantPet(childId, drawResult.petType(), drawResult.grade());
            if (childProfile.getEquippedPetId() == null) {
                childProfile.equipPet(pet.getId());
            }
        } else {
            fragmentsGot = FRAGMENT_REWARDS.get(drawResult.grade());
            Fragment fragment = fragmentRepository.findWithLockByChildIdAndGrade(childId, drawResult.grade())
                    .orElseGet(() -> Fragment.create(childId, drawResult.grade()));
            fragment.add(fragmentsGot);
            fragmentRepository.save(fragment);
        }

        gachaPullRepository.save(GachaPull.create(
                childId,
                ticketType,
                drawResult.petType(),
                drawResult.grade(),
                isNew,
                pet == null ? null : pet.getId(),
                fragmentsGot,
                srMissBefore
        ));

        return new GachaPullResponse(
                new GachaPullResponse.Result(
                        pet == null ? null : pet.getId(),
                        drawResult.petType(),
                        drawResult.petType(),
                        drawResult.grade().name(),
                        isNew,
                        fragmentsGot
                ),
                childProfile.getSrMissCount(),
                drawResult.appliedSrBonus(),
                levelUp,
                remainingTickets(childId)
        );
    }

    @Transactional(readOnly = true)
    public FragmentListResponse getFragments(UUID childId) {
        childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        return new FragmentListResponse(Arrays.stream(PetGrade.values())
                .map(grade -> new FragmentListResponse.FragmentSummary(
                        grade.name(),
                        fragmentRepository.findById(new FragmentId(childId, grade))
                                .map(Fragment::getCount)
                                .orElse(0),
                        EXCHANGE_THRESHOLDS.get(grade)
                ))
                .toList());
    }

    @Transactional
    public GachaExchangeResponse exchange(UUID childId, PetGrade grade, String petType) {
        childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!gachaProbabilityService.isValidPetTypeForGrade(grade, petType)) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "유효하지 않은 펫 종류예요");
        }
        if (petRepository.existsByChildIdAndPetType(childId, petType)) {
            throw new AimongException(ErrorCode.CONFLICT, "이미 보유한 펫이에요");
        }

        Fragment fragment = fragmentRepository.findWithLockByChildIdAndGrade(childId, grade)
                .orElseThrow(() -> new AimongException(ErrorCode.BAD_REQUEST, "조각이 부족해요!"));
        int threshold = EXCHANGE_THRESHOLDS.get(grade);
        if (!fragment.canSpend(threshold)) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "조각이 부족해요!");
        }

        fragment.spend(threshold);
        Pet pet = petService.grantPet(childId, petType, grade);
        return new GachaExchangeResponse(pet.getId(), pet.getPetType(), pet.getGrade().name(), pet.getStage().name());
    }

    private boolean crossedGachaLevelBoundary(int beforePullCount, int afterPullCount) {
        return beforePullCount < 20 && afterPullCount >= 20
                || beforePullCount < 50 && afterPullCount >= 50
                || beforePullCount < 100 && afterPullCount >= 100;
    }

    private GachaPullResponse.RemainingTickets remainingTickets(UUID childId) {
        return new GachaPullResponse.RemainingTickets(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL)),
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.RARE)),
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.EPIC))
        );
    }
}
