package com.aimong.backend.domain.streak.service;

import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.streak.entity.MilestoneReward;
import com.aimong.backend.domain.streak.entity.StreakMilestone;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.MilestoneRewardRepository;
import com.aimong.backend.domain.streak.repository.StreakMilestoneRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final TicketRepository ticketRepository;
    private final MilestoneRewardRepository milestoneRewardRepository;
    private final StreakMilestoneRepository streakMilestoneRepository;

    @Transactional
    public List<SubmitResponse.RewardResponse> applyStreakRewards(UUID childId, StreakRecord streakRecord) {
        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>();
        rewards.addAll(applyFixedStreakRewards(childId, streakRecord.getContinuousDays()));
        rewards.addAll(applyUserGoalRewards(childId, streakRecord.getContinuousDays()));
        return rewards;
    }

    private List<SubmitResponse.RewardResponse> applyFixedStreakRewards(UUID childId, int continuousDays) {
        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>();
        if (continuousDays == 7 && !milestoneRewardRepository.existsByChildIdAndMilestoneDays(childId, (short) 7)) {
            grantTickets(childId, TicketType.RARE, 1);
            milestoneRewardRepository.save(MilestoneReward.create(childId, (short) 7));
            rewards.add(ticketReward("RARE", 1, "STREAK_MILESTONE_DAY7"));
        }
        if (continuousDays == 30 && !milestoneRewardRepository.existsByChildIdAndMilestoneDays(childId, (short) 30)) {
            grantTickets(childId, TicketType.EPIC, 1);
            milestoneRewardRepository.save(MilestoneReward.create(childId, (short) 30));
            rewards.add(ticketReward("EPIC", 1, "STREAK_MILESTONE_DAY30"));
        }
        return rewards;
    }

    private List<SubmitResponse.RewardResponse> applyUserGoalRewards(UUID childId, int continuousDays) {
        if (continuousDays <= 30) {
            return List.of();
        }

        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>();
        List<StreakMilestone> milestones = streakMilestoneRepository
                .findAllByChildIdAndRewardClaimedFalseAndTargetDaysLessThanEqual(childId, (short) continuousDays);
        for (StreakMilestone milestone : milestones) {
            TicketGrant grant = grantForTier(milestone.getTier());
            grantTickets(childId, grant.ticketType(), grant.count());
            milestone.achieveAndClaim();
            rewards.add(ticketReward(
                    grant.ticketType().name(),
                    grant.count(),
                    "STREAK_GOAL_TIER" + milestone.getTier() + "_DAY" + milestone.getTargetDays()
            ));
        }
        return rewards;
    }

    private TicketGrant grantForTier(short tier) {
        return switch (tier) {
            case 1 -> new TicketGrant(TicketType.NORMAL, 1);
            case 2 -> new TicketGrant(TicketType.RARE, 1);
            case 3 -> new TicketGrant(TicketType.RARE, 3);
            default -> throw new IllegalStateException("Unsupported streak milestone tier: " + tier);
        };
    }

    private SubmitResponse.RewardResponse ticketReward(String ticketType, int count, String reason) {
        return new SubmitResponse.RewardResponse(
                "TICKET",
                ticketType,
                count,
                null,
                reason
        );
    }

    private void grantTickets(UUID childId, TicketType ticketType, int count) {
        ticketRepository.saveAll(IntStream.range(0, count)
                .mapToObj(index -> Ticket.issue(childId, ticketType))
                .toList());
    }

    private record TicketGrant(
            TicketType ticketType,
            int count
    ) {
    }
}
