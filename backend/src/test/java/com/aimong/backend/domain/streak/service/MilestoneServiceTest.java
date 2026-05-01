package com.aimong.backend.domain.streak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.streak.entity.StreakMilestone;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.MilestoneRewardRepository;
import com.aimong.backend.domain.streak.repository.StreakMilestoneRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private MilestoneRewardRepository milestoneRewardRepository;
    @Mock private StreakMilestoneRepository streakMilestoneRepository;

    @Test
    void applyStreakRewardsGrantsRareTicketOnceAtDay7() {
        UUID childId = UUID.randomUUID();
        StreakRecord streakRecord = StreakRecord.create(childId);
        LocalDate startDate = LocalDate.of(2026, 4, 24);
        for (int i = 0; i < 7; i++) {
            streakRecord.recordMissionCompletion(startDate.plusDays(i));
        }

        when(milestoneRewardRepository.existsByChildIdAndMilestoneDays(childId, (short) 7)).thenReturn(false);
        when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var rewards = service().applyStreakRewards(childId, streakRecord);

        assertThat(rewards).singleElement().satisfies(reward -> {
            assertThat(reward.type()).isEqualTo("TICKET");
            assertThat(reward.ticketType()).isEqualTo("RARE");
            assertThat(reward.count()).isEqualTo(1);
            assertThat(reward.reason()).isEqualTo("STREAK_MILESTONE_DAY7");
        });
        verify(milestoneRewardRepository).save(any());
    }

    @Test
    void applyStreakRewardsClaimsReachedUserGoalMilestones() {
        UUID childId = UUID.randomUUID();
        StreakRecord streakRecord = StreakRecord.create(childId);
        LocalDate startDate = LocalDate.of(2026, 3, 7);
        for (int i = 0; i < 55; i++) {
            streakRecord.recordMissionCompletion(startDate.plusDays(i));
        }
        StreakMilestone milestone = StreakMilestone.create(childId, (short) 55, (short) 2);

        when(streakMilestoneRepository.findAllByChildIdAndRewardClaimedFalseAndTargetDaysLessThanEqual(childId, (short) 55))
                .thenReturn(List.of(milestone));
        when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var rewards = service().applyStreakRewards(childId, streakRecord);

        assertThat(milestone.isAchieved()).isTrue();
        assertThat(milestone.isRewardClaimed()).isTrue();
        assertThat(rewards).singleElement().satisfies(reward -> {
            assertThat(reward.ticketType()).isEqualTo("RARE");
            assertThat(reward.count()).isEqualTo(1);
            assertThat(reward.reason()).isEqualTo("STREAK_GOAL_TIER2_DAY55");
        });
        verify(ticketRepository).saveAll(any(Iterable.class));
    }

    private MilestoneService service() {
        return new MilestoneService(ticketRepository, milestoneRewardRepository, streakMilestoneRepository);
    }
}
