package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionStatusResponse;
import com.aimong.backend.domain.mission.dto.ReviveAttemptRequest;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.entity.QuizAttemptStatus;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.reward.repository.CurrencyTransactionRepository;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock private MissionRepository missionRepository;
    @Mock private MissionSetRepository missionSetRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;
    @Mock private MissionService missionService;
    @Mock private QuizService quizService;
    @Mock private CurrencyTransactionRepository currencyTransactionRepository;

    @Test
    void getMissionStatusReusesSingleMissionSetAvailability() {
        QuizAttemptService service = service();
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        MissionSet starOneSet = missionSet("S0101-1-1", 1);
        MissionSet starTwoSet = missionSet("S0101-2-1", 2);
        ChildProfile child = ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
        MissionSetProgress progress = MissionSetProgress.create(
                childId,
                starOneSet.getSetId(),
                missionId,
                1,
                1,
                1,
                UUID.randomUUID(),
                100,
                10
        );
        MissionService.MissionSetAvailability availability = new MissionService.MissionSetAvailability(
                List.of(starOneSet, starTwoSet),
                List.of(starOneSet, starTwoSet),
                Map.of(starOneSet.getSetId(), progress)
        );

        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mission));
        when(mission.isActive()).thenReturn(true);
        when(mission.getId()).thenReturn(missionId);
        when(mission.getMissionCode()).thenReturn("S0101");
        when(mission.getTitle()).thenReturn("Mission");
        when(missionSetRepository.findAllByMissionIdAndActiveTrueOrderByStarLevelAscVariantNoAscSetIdAsc(missionId))
                .thenReturn(List.of(starOneSet, starTwoSet));
        when(missionService.missionSetAvailability(childId)).thenReturn(availability);
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(child));
        when(quizAttemptRepository.findFirstByChildIdAndMissionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(childId),
                eq(missionId),
                eq(QuizAttemptStatus.IN_PROGRESS),
                any(Instant.class)
        )).thenReturn(Optional.empty());

        MissionStatusResponse response = service.getMissionStatus(childId, missionId);

        assertThat(response.isUnlocked()).isTrue();
        assertThat(response.starLevels()).hasSize(2);
        assertThat(response.starLevels().get(0).completedSetCount()).isEqualTo(1);
        assertThat(response.starLevels().get(1).isPlayable()).isTrue();
        verify(missionService).missionSetAvailability(childId);
        verify(missionService, never()).isUnlocked(eq(childId), any(MissionSet.class));
        verify(missionService, never()).isStarLevelPlayable(eq(childId), eq(missionId), anyInt());
    }

    @Test
    void reviveConsumesGearAndRestoresLives() {
        QuizAttemptService service = service();
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.create(
                childId,
                missionId,
                "S0101-L1",
                1,
                "[]",
                Instant.now().plusSeconds(600),
                false
        );
        attempt.recordWrongAnswer();
        attempt.recordWrongAnswer();
        attempt.recordWrongAnswer();
        ChildProfile child = ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
        child.addGear(20);

        when(quizAttemptRepository.findWithLockById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(child));

        var response = service.revive(childId, attempt.getId(), new ReviveAttemptRequest(true));

        assertThat(response.remainingLives()).isEqualTo(3);
        assertThat(response.reviveCount()).isEqualTo(1);
        assertThat(response.reviveCost()).isEqualTo(10);
        assertThat(response.gearBalance()).isEqualTo(10);
        verify(currencyTransactionRepository).save(org.mockito.ArgumentMatchers.any());
        verify(childActivityService).touchLastActiveAt(childId);
    }

    private QuizAttemptService service() {
        return new QuizAttemptService(
                missionRepository,
                missionSetRepository,
                quizAttemptRepository,
                childProfileRepository,
                childActivityService,
                missionService,
                quizService,
                new CurrencyService(currencyTransactionRepository),
                new ObjectMapper()
        );
    }

    private MissionSet missionSet(String setId, int starLevel) {
        MissionSet missionSet = org.mockito.Mockito.mock(MissionSet.class);
        when(missionSet.getSetId()).thenReturn(setId);
        when(missionSet.getStarLevel()).thenReturn(starLevel);
        return missionSet;
    }
}
