package com.aimong.backend.domain.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.chat.repository.ChatUsageRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import com.aimong.backend.domain.quest.repository.DailyQuestRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyQuestServiceTest {

    @Mock private DailyQuestRepository dailyQuestRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private MissionAttemptRepository missionAttemptRepository;
    @Mock private ChatUsageRepository chatUsageRepository;

    @Test
    void missionOneCompletionDoesNotAutoClaimReward() {
        UUID childId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 5, 27);
        ChildProfile childProfile = org.mockito.Mockito.mock(ChildProfile.class);
        DailyQuestService service = new DailyQuestService(
                dailyQuestRepository,
                childProfileRepository,
                missionAttemptRepository,
                chatUsageRepository
        );

        when(dailyQuestRepository.findByChildIdAndQuestDateAndQuestType(
                org.mockito.Mockito.eq(childId),
                org.mockito.Mockito.eq(today),
                org.mockito.Mockito.any(DailyQuestType.class)
        )).thenAnswer(invocation -> Optional.of(DailyQuest.create(
                childId,
                today,
                invocation.getArgument(2)
        )));
        when(missionAttemptRepository.countByChildIdAndAttemptDateAndReviewFalseAndPassedTrue(childId, today))
                .thenReturn(1L);
        when(chatUsageRepository.findByChildIdAndUsageDate(childId, today)).thenReturn(Optional.empty());
        when(childProfile.getTodayXp()).thenReturn(0);

        var quests = service.refreshDailyProgress(childId, childProfile, today);

        DailyQuest missionOne = quests.get(DailyQuestType.MISSION_1);
        assertThat(missionOne.isCompleted()).isTrue();
        assertThat(missionOne.isRewardClaimed()).isFalse();
        assertThat(quests).containsKeys(
                DailyQuestType.MISSION_3,
                DailyQuestType.STREAK_CHECK,
                DailyQuestType.ALL_DAILY
        );
        assertThat(quests).doesNotContainKey(DailyQuestType.ALL_3);
        assertThat(quests.get(DailyQuestType.STREAK_CHECK).isCompleted()).isTrue();
        assertThat(DailyQuestService.claimType(DailyQuestType.MISSION_1)).isEqualTo("MANUAL");
    }
}
