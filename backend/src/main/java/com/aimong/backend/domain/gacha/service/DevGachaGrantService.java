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
        grantFragments(childId, request.normalFragments()
                + request.rareFragments()
                + request.epicFragments()
                + request.legendFragments());

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

    private void grantFragments(UUID childId, int count) {
        if (count <= 0) {
            return;
        }
        Fragment fragment = fragmentRepository.findWithLockByChildId(childId)
                .orElseGet(() -> Fragment.create(childId));
        fragment.add(count);
        fragmentRepository.save(fragment);
    }

    private GachaPullResponse.RemainingTickets remainingTickets(UUID childId) {
        return new GachaPullResponse.RemainingTickets(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL))
        );
    }

    private FragmentListResponse fragments(UUID childId) {
        int totalCount = fragmentRepository.findByChildId(childId)
                .map(Fragment::getCount)
                .orElse(0);
        return new FragmentListResponse(totalCount, Arrays.stream(PetGrade.values())
                .map(grade -> new FragmentListResponse.FragmentSummary(
                        grade.name(),
                        totalCount,
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
