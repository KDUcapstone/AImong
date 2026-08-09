package com.aimong.backend.domain.stagereward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.domain.reward.entity.CurrencyTransactionReason;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.aimong.backend.domain.stagereward.entity.StageCompletionReward;
import com.aimong.backend.domain.stagereward.repository.StageCompletionRewardRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StageCompletionRewardServiceTest {

    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private StageCompletionRewardRepository stageCompletionRewardRepository;
    @Mock private MissionSetRepository missionSetRepository;
    @Mock private MissionSetProgressRepository missionSetProgressRepository;
    @Mock private CurrencyService currencyService;
    @Mock private TicketRepository ticketRepository;

    @Test
    void triggerIfStageCompletedGrantsDefaultGearOnce() {
        ChildProfile childProfile = ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
        UUID attemptId = UUID.randomUUID();
        when(missionSetRepository.countActiveStageStarOneMissionIds((short) 2)).thenReturn(6L);
        when(missionSetProgressRepository.countCompletedStageStarOneMissionIds(childProfile.getId(), 2)).thenReturn(6L);
        when(stageCompletionRewardRepository.findWithLockByChildProfileIdAndStageNumber(childProfile.getId(), 2))
                .thenReturn(Optional.empty());
        when(stageCompletionRewardRepository.save(any(StageCompletionReward.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().triggerIfStageCompleted(childProfile, 2, attemptId);

        assertThat(response).isNotNull();
        assertThat(response.stageNumber()).isEqualTo(2);
        assertThat(response.defaultGearReward()).isEqualTo(50);
        assertThat(response.normalTicketReward()).isZero();
        verify(currencyService).grantGear(
                eq(childProfile),
                eq(50),
                eq(CurrencyTransactionReason.STAGE_REWARD_GEAR),
                eq("STAGE_COMPLETION"),
                eq(attemptId.toString())
        );
        verify(ticketRepository, never()).saveAll(any(Iterable.class));
    }

    private StageCompletionRewardService service() {
        return new StageCompletionRewardService(
                childProfileRepository,
                stageCompletionRewardRepository,
                missionSetRepository,
                missionSetProgressRepository,
                currencyService,
                ticketRepository
        );
    }
}
