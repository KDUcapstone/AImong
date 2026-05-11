package com.aimong.backend.domain.reward.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.reward.dto.ReturnRewardClaimResponse;
import com.aimong.backend.domain.reward.dto.ReturnRewardResponse;
import com.aimong.backend.domain.reward.entity.ReturnRewardClaim;
import com.aimong.backend.domain.reward.repository.ReturnRewardClaimRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReturnRewardService {

    private static final int MIN_DAYS_MISSED = 3;
    private static final int MAX_TICKET_COUNT = 3;

    private final StreakRecordRepository streakRecordRepository;
    private final ReturnRewardClaimRepository returnRewardClaimRepository;
    private final TicketRepository ticketRepository;
    private final ChildActivityService childActivityService;

    @Transactional
    public ReturnRewardResponse getReturnReward(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        return calculateReward(childId)
                .map(eligibility -> ReturnRewardResponse.hasReward(
                        eligibility.daysMissed(),
                        eligibility.ticketCount()
                ))
                .orElseGet(ReturnRewardResponse::noReward);
    }

    @Transactional
    public ReturnRewardClaimResponse claimReturnReward(UUID childId) {
        childActivityService.touchLastActiveAt(childId);

        RewardEligibility eligibility = calculateRewardForClaim(childId);
        returnRewardClaimRepository.save(ReturnRewardClaim.create(
                childId,
                eligibility.baseDate(),
                eligibility.ticketCount()
        ));

        for (int i = 0; i < eligibility.ticketCount(); i++) {
            ticketRepository.save(Ticket.issue(childId, TicketType.NORMAL));
        }

        return new ReturnRewardClaimResponse(
                List.of(new ReturnRewardClaimResponse.RewardItem(
                        "TICKET",
                        TicketType.NORMAL.name(),
                        eligibility.ticketCount(),
                        "RETURN_REWARD"
                )),
                remainingTickets(childId)
        );
    }

    private java.util.Optional<RewardEligibility> calculateReward(UUID childId) {
        return streakRecordRepository.findById(childId)
                .flatMap(streak -> calculateReward(childId, streak));
    }

    private RewardEligibility calculateRewardForClaim(UUID childId) {
        StreakRecord streak = streakRecordRepository.findWithLockByChildId(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.BAD_REQUEST, "No return reward available."));
        return calculateReward(childId, streak)
                .orElseThrow(() -> {
                    if (streak.getLastCompletedDate() != null
                            && returnRewardClaimRepository.existsByChildIdAndBaseLastCompletedDate(
                            childId,
                            streak.getLastCompletedDate()
                    )) {
                        return new AimongException(ErrorCode.CONFLICT, "Return reward already claimed.");
                    }
                    return new AimongException(ErrorCode.BAD_REQUEST, "No return reward available.");
                });
    }

    private java.util.Optional<RewardEligibility> calculateReward(UUID childId, StreakRecord streak) {
        LocalDate baseDate = streak.getLastCompletedDate();
        if (baseDate == null) {
            return java.util.Optional.empty();
        }

        long daysMissed = ChronoUnit.DAYS.between(baseDate, KstDateUtils.today());
        if (daysMissed < MIN_DAYS_MISSED) {
            return java.util.Optional.empty();
        }
        if (returnRewardClaimRepository.existsByChildIdAndBaseLastCompletedDate(childId, baseDate)) {
            return java.util.Optional.empty();
        }

        int ticketCount = Math.toIntExact(Math.min(daysMissed - 2, MAX_TICKET_COUNT));
        return java.util.Optional.of(new RewardEligibility(baseDate, daysMissed, ticketCount));
    }

    private ReturnRewardClaimResponse.RemainingTickets remainingTickets(UUID childId) {
        return new ReturnRewardClaimResponse.RemainingTickets(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL)),
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.RARE)),
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.EPIC))
        );
    }

    private record RewardEligibility(
            LocalDate baseDate,
            long daysMissed,
            int ticketCount
    ) {
    }
}
