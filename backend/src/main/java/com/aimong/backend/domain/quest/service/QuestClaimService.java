package com.aimong.backend.domain.quest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.quest.dto.ClaimResponse;
import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import com.aimong.backend.domain.quest.entity.WeeklyQuest;
import com.aimong.backend.domain.quest.entity.WeeklyQuestType;
import com.aimong.backend.domain.quest.repository.DailyQuestRepository;
import com.aimong.backend.domain.quest.repository.WeeklyQuestRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestClaimService {

    private static final String PERIOD_DAILY = "daily";
    private static final String PERIOD_WEEKLY = "weekly";

    private final DailyQuestRepository dailyQuestRepository;
    private final WeeklyQuestRepository weeklyQuestRepository;
    private final ChildProfileRepository childProfileRepository;
    private final TicketRepository ticketRepository;
    private final DailyQuestService dailyQuestService;
    private final WeeklyQuestService weeklyQuestService;

    @Transactional
    public ClaimResponse claim(UUID childId, String questType, String period) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));

        if (PERIOD_DAILY.equals(period)) {
            return claimDaily(childId, childProfile, parseDailyQuestType(questType));
        }
        if (PERIOD_WEEKLY.equals(period)) {
            return claimWeekly(childId, childProfile, parseWeeklyQuestType(questType));
        }
        throw new AimongException(ErrorCode.BAD_REQUEST, "period must be daily or weekly.");
    }

    private ClaimResponse claimDaily(UUID childId, ChildProfile childProfile, DailyQuestType questType) {
        if (!DailyQuestService.isActiveType(questType)) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "Inactive daily quest type.");
        }

        LocalDate today = KstDateUtils.today();
        dailyQuestService.refreshDailyProgress(childId, childProfile, today);
        DailyQuest quest = dailyQuestRepository.findWithLockByChildIdAndQuestDateAndQuestType(childId, today, questType)
                .orElseThrow(() -> new AimongException(ErrorCode.BAD_REQUEST, "Daily quest is not completed."));
        validateClaimable(quest.isCompleted(), quest.isRewardClaimed());
        quest.claimReward();

        TicketType ticketType = TicketType.NORMAL;
        int count = dailyRewardCount(questType);
        grantTickets(childId, ticketType, count);
        return new ClaimResponse(
                List.of(new ClaimResponse.RewardResponse("TICKET", ticketType.name(), count, "DAILY_QUEST_" + questType.name())),
                remainingTickets(childId)
        );
    }

    private ClaimResponse claimWeekly(UUID childId, ChildProfile childProfile, WeeklyQuestType questType) {
        LocalDate weekStart = KstDateUtils.currentWeekStart();
        weeklyQuestService.refreshWeeklyProgress(childId, childProfile, weekStart);
        WeeklyQuest quest = weeklyQuestRepository.findWithLockByChildIdAndWeekStartAndQuestType(childId, weekStart, questType)
                .orElseThrow(() -> new AimongException(ErrorCode.BAD_REQUEST, "Weekly quest is not completed."));
        validateClaimable(quest.isCompleted(), quest.isRewardClaimed());
        quest.claimReward();

        TicketReward reward = weeklyReward(questType);
        grantTickets(childId, reward.ticketType(), reward.count());
        return new ClaimResponse(
                List.of(new ClaimResponse.RewardResponse("TICKET", reward.ticketType().name(), reward.count(), "WEEKLY_QUEST_" + questType.name())),
                remainingTickets(childId)
        );
    }

    private void validateClaimable(boolean completed, boolean rewardClaimed) {
        if (!completed) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "Quest is not completed.");
        }
        if (rewardClaimed) {
            throw new AimongException(ErrorCode.CONFLICT, "Quest reward is already claimed.");
        }
    }

    private DailyQuestType parseDailyQuestType(String questType) {
        try {
            return DailyQuestType.valueOf(questType);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "Unknown daily quest type.");
        }
    }

    private WeeklyQuestType parseWeeklyQuestType(String questType) {
        try {
            return WeeklyQuestType.valueOf(questType);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "Unknown weekly quest type.");
        }
    }

    private int dailyRewardCount(DailyQuestType questType) {
        return switch (questType) {
            case ALL_DAILY -> 2;
            case MISSION_1, MISSION_3, XP_20, CHAT_GPT, STREAK_CHECK -> 1;
            case ALL_3 -> throw new AimongException(ErrorCode.BAD_REQUEST, "Inactive daily quest type.");
        };
    }

    private TicketReward weeklyReward(WeeklyQuestType questType) {
        return switch (questType) {
            case MISSION_10 -> new TicketReward(TicketType.NORMAL, 3);
            case XP_100, MISSION_5, STREAK_5 -> new TicketReward(TicketType.NORMAL, 2);
            case CHAT_3 -> new TicketReward(TicketType.NORMAL, 1);
        };
    }

    private void grantTickets(UUID childId, TicketType ticketType, int count) {
        ticketRepository.saveAll(IntStream.range(0, count)
                .mapToObj(index -> Ticket.issue(childId, ticketType))
                .toList());
    }

    private ClaimResponse.RemainingTicketsResponse remainingTickets(UUID childId) {
        return new ClaimResponse.RemainingTicketsResponse(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL))
        );
    }

    private record TicketReward(TicketType ticketType, int count) {
    }
}
