package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock private MissionRepository missionRepository;
    @Mock private MissionAttemptRepository missionAttemptRepository;
    @Mock private ChildActivityService childActivityService;

    @Test
    void getMissionsUsesPassedNonReviewAttemptsForCompletionState() {
        MissionService missionService = new MissionService(
                missionRepository,
                missionAttemptRepository,
                childActivityService
        );

        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        LocalDate completedAt = LocalDate.of(2026, 4, 25);
        Mission mission = org.mockito.Mockito.mock(Mission.class);

        when(missionAttemptRepository.countCompletedMissionByStage(childId, (short) 1)).thenReturn(3L);
        when(missionAttemptRepository.countCompletedMissionByStage(childId, (short) 2)).thenReturn(1L);
        when(missionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)).thenReturn(0L);
        when(missionAttemptRepository.findLatestCompletedAt(childId, missionId)).thenReturn(Optional.of(completedAt));
        when(missionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc()).thenReturn(List.of(mission));
        when(mission.getId()).thenReturn(missionId);
        when(mission.getStage()).thenReturn((short) 2);
        when(mission.getTitle()).thenReturn("Mission");
        when(mission.getDescription()).thenReturn("Description");

        MissionListResponse response = missionService.getMissions(childId);

        assertThat(response.stageProgress().stage1Completed()).isEqualTo(3);
        assertThat(response.stageProgress().stage2Completed()).isEqualTo(1);
        assertThat(response.missions()).singleElement().satisfies(summary -> {
            assertThat(summary.isUnlocked()).isTrue();
            assertThat(summary.isCompleted()).isTrue();
            assertThat(summary.completedAt()).isEqualTo(completedAt);
            assertThat(summary.isReviewable()).isTrue();
        });
        verify(childActivityService).touchLastActiveAt(childId);
    }
}
