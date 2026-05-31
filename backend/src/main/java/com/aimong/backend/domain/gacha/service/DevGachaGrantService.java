package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.dto.DevGachaGrantRequest;
import com.aimong.backend.domain.gacha.dto.DevGachaGrantResponse;
import com.aimong.backend.domain.gacha.dto.FragmentListResponse;
import com.aimong.backend.domain.gacha.dto.GachaPullResponse;
import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.FragmentRepository;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "dev", "test"})
@RequiredArgsConstructor
public class DevGachaGrantService {

    private final ChildProfileRepository childProfileRepository;
    private final TicketRepository ticketRepository;
    private final FragmentRepository fragmentRepository;

    @Transactional
    public DevGachaGrantResponse grant(UUID childId, DevGachaGrantRequest request) {
        childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));

        grantTickets(childId, TicketType.NORMAL, request.normalTickets());

        grantFragments(childId, PetGrade.NORMAL, request.normalFragments());
        grantFragments(childId, PetGrade.RARE, request.rareFragments());
        grantFragments(childId, PetGrade.EPIC, request.epicFragments());
        grantFragments(childId, PetGrade.LEGEND, request.legendFragments());

        return new DevGachaGrantResponse(remainingTickets(childId), fragments(childId));
    }

    private void grantTickets(UUID childId, TicketType ticketType, int count) {
        if (count <= 0) {
            return;
        }
        ticketRepository.saveAll(IntStream.range(0, count)
                .mapToObj(index -> Ticket.issue(childId, ticketType))
                .toList());
    }

    private void grantFragments(UUID childId, PetGrade grade, int count) {
        if (count <= 0) {
            return;
        }
        Fragment fragment = fragmentRepository.findWithLockByChildIdAndGrade(childId, grade)
                .orElseGet(() -> Fragment.create(childId, grade));
        fragment.add(count);
        fragmentRepository.save(fragment);
    }

    private GachaPullResponse.RemainingTickets remainingTickets(UUID childId) {
        return new GachaPullResponse.RemainingTickets(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL))
        );
    }

    private FragmentListResponse fragments(UUID childId) {
        List<Fragment> fragments = fragmentRepository.findByChildId(childId);
        int totalCount = fragments.stream()
                .mapToInt(Fragment::getCount)
                .sum();
        return new FragmentListResponse(totalCount, Arrays.stream(PetGrade.values())
                .map(grade -> new FragmentListResponse.FragmentSummary(
                        grade.name(),
                        fragments.stream()
                                .filter(fragment -> fragment.getGrade() == grade)
                                .findFirst()
                                .map(Fragment::getCount)
                                .orElse(0),
                        exchangeThreshold(grade)
                ))
                .toList());
    }

    private int exchangeThreshold(PetGrade grade) {
        return switch (grade) {
            case NORMAL -> 10;
            case RARE -> 30;
            case EPIC -> 80;
            case LEGEND -> 200;
        };
    }
}
