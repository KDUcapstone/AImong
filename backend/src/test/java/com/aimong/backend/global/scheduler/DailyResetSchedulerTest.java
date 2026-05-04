package com.aimong.backend.global.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyResetSchedulerTest {

    @Mock private StreakRecordRepository streakRecordRepository;
    @Mock private ChildProfileRepository childProfileRepository;

    @Test
    void processStreakResetConsumesShieldBeforeResettingStreak() {
        UUID childId = UUID.randomUUID();
        StreakRecord streakRecord = StreakRecord.create(childId);
        streakRecord.recordMissionCompletion(LocalDate.of(2026, 4, 28));
        ChildProfile profile = org.mockito.Mockito.mock(ChildProfile.class);
        when(streakRecordRepository.findStaleTodayMissionCounts(any()))
                .thenReturn(List.of(streakRecord));
        when(streakRecordRepository.findMissedActiveStreaksForReset(any()))
                .thenReturn(List.of(streakRecord));
        when(childProfileRepository.findWithLockById(childId)).thenReturn(java.util.Optional.of(profile));
        when(profile.consumeShieldIfAvailable()).thenReturn(true);

        new DailyResetScheduler(streakRecordRepository, childProfileRepository).processStreakReset();

        assertThat(streakRecord.getContinuousDays()).isEqualTo(1);
        assertThat(streakRecord.getTodayMissionCount()).isZero();
    }

    @Test
    void processStreakResetClearsStreakWhenNoShieldIsAvailable() {
        UUID childId = UUID.randomUUID();
        StreakRecord streakRecord = StreakRecord.create(childId);
        streakRecord.recordMissionCompletion(LocalDate.of(2026, 4, 28));
        when(streakRecordRepository.findStaleTodayMissionCounts(any()))
                .thenReturn(List.of(streakRecord));
        when(streakRecordRepository.findMissedActiveStreaksForReset(any()))
                .thenReturn(List.of(streakRecord));
        when(childProfileRepository.findWithLockById(childId)).thenReturn(java.util.Optional.empty());

        new DailyResetScheduler(streakRecordRepository, childProfileRepository).processStreakReset();

        assertThat(streakRecord.getContinuousDays()).isZero();
        assertThat(streakRecord.getTodayMissionCount()).isZero();
    }
}
