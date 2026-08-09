package com.aimong.backend.global.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyMissionSchedulerTest {

    @Mock private MissionSetRepository missionSetRepository;

    @Test
    void verifyNextDayMissionSetsReadyChecksExpectedFixedSetCount() {
        when(missionSetRepository.countByActiveTrue()).thenReturn(96L);

        scheduler().verifyNextDayMissionSetsReady();

        verify(missionSetRepository).countByActiveTrue();
    }

    @Test
    void verifyNextDayMissionSetsReadyDoesNotFailWhenSetCountIsUnexpected() {
        when(missionSetRepository.countByActiveTrue()).thenReturn(95L);

        scheduler().verifyNextDayMissionSetsReady();

        verify(missionSetRepository).countByActiveTrue();
    }

    private DailyMissionScheduler scheduler() {
        return new DailyMissionScheduler(missionSetRepository);
    }
}
