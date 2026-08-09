package com.aimong.backend.domain.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.reward.entity.ReturnRewardClaim;
import com.aimong.backend.domain.reward.repository.ReturnRewardClaimRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReturnRewardServiceTest {

    @Mock private StreakRecordRepository streakRecordRepository;
    @Mock private ReturnRewardClaimRepository returnRewardClaimRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private ChildActivityService childActivityService;

    @Test
    void getReturnRewardReturnsTicketCountFromMissedDays() {
        ChildProfile child = child();
        StreakRecord streak = streakWithLastCompletedDate(child, 4);
        when(streakRecordRepository.findById(child.getId())).thenReturn(Optional.of(streak));
        when(returnRewardClaimRepository.existsByChildIdAndBaseLastCompletedDate(
                child.getId(),
                KstDateUtils.today().minusDays(4)
        )).thenReturn(false);

        var response = service().getReturnReward(child.getId());

        assertThat(response.hasReward()).isTrue();
        assertThat(response.daysMissed()).isEqualTo(4);
        assertThat(response.ticketCount()).isEqualTo(2);
        verify(childActivityService).touchLastActiveAt(child.getId());
    }

    @Test
    void claimReturnRewardCreatesClaimAndIssuesNormalTickets() {
        ChildProfile child = child();
        StreakRecord streak = streakWithLastCompletedDate(child, 5);
        when(streakRecordRepository.findWithLockByChildId(child.getId())).thenReturn(Optional.of(streak));
        when(returnRewardClaimRepository.existsByChildIdAndBaseLastCompletedDate(
                child.getId(),
                KstDateUtils.today().minusDays(5)
        )).thenReturn(false);
        when(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(eq(child.getId()), any(TicketType.class)))
                .thenReturn(3L, 0L, 0L);

        var response = service().claimReturnReward(child.getId());

        ArgumentCaptor<ReturnRewardClaim> claimCaptor = ArgumentCaptor.forClass(ReturnRewardClaim.class);
        verify(returnRewardClaimRepository).save(claimCaptor.capture());
        assertThat(claimCaptor.getValue().getTicketCount()).isEqualTo(3);
        verify(ticketRepository, times(3)).save(any(Ticket.class));
        assertThat(response.rewards()).hasSize(1);
        assertThat(response.rewards().get(0).ticketType()).isEqualTo("NORMAL");
        assertThat(response.rewards().get(0).count()).isEqualTo(3);
        assertThat(response.remainingTickets().normal()).isEqualTo(3);
        verify(childActivityService).touchLastActiveAt(child.getId());
    }

    @Test
    void claimReturnRewardRejectsAlreadyClaimedBaseDate() {
        ChildProfile child = child();
        StreakRecord streak = streakWithLastCompletedDate(child, 3);
        when(streakRecordRepository.findWithLockByChildId(child.getId())).thenReturn(Optional.of(streak));
        when(returnRewardClaimRepository.existsByChildIdAndBaseLastCompletedDate(
                child.getId(),
                KstDateUtils.today().minusDays(3)
        )).thenReturn(true);

        assertThatThrownBy(() -> service().claimReturnReward(child.getId()))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    private ReturnRewardService service() {
        return new ReturnRewardService(
                streakRecordRepository,
                returnRewardClaimRepository,
                ticketRepository,
                childActivityService
        );
    }

    private ChildProfile child() {
        return ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
    }

    private StreakRecord streakWithLastCompletedDate(ChildProfile child, int daysAgo) {
        StreakRecord streak = StreakRecord.create(child.getId());
        streak.recordMissionCompletion(KstDateUtils.today().minusDays(daysAgo));
        return streak;
    }
}
