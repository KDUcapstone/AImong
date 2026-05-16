package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
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
    @Mock private MissionSetRepository missionSetRepository;
    @Mock private MissionSetProgressRepository missionSetProgressRepository;
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

        when(missionAttemptRepository.countCompletedMissionByStage(childId, (short) 1)).thenReturn(5L);
        when(missionAttemptRepository.countCompletedMissionByStage(childId, (short) 2)).thenReturn(1L);
        when(missionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)).thenReturn(0L);
        when(missionAttemptRepository.findLatestCompletedAt(childId, missionId)).thenReturn(Optional.of(completedAt));
        when(missionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc()).thenReturn(List.of(mission));
        when(mission.getId()).thenReturn(missionId);
        when(mission.getStage()).thenReturn((short) 2);
        when(mission.getTitle()).thenReturn("Mission");
        when(mission.getDescription()).thenReturn("Description");

        MissionListResponse response = missionService.getMissions(childId);

        assertThat(response.progress().completedSetCount()).isEqualTo(6);
        assertThat(response.stages()).singleElement().satisfies(stage -> assertThat(stage.stage()).isEqualTo(2));
        assertThat(response.stages().getFirst().missions()).singleElement().satisfies(summary -> {
            assertThat(summary.isUnlocked()).isTrue();
            assertThat(summary.starLevels()).singleElement().satisfies(starLevel -> {
                assertThat(starLevel.completedSetCount()).isEqualTo(1);
                assertThat(starLevel.isReviewable()).isTrue();
            });
        });
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void starLevelOpensOnlyAfterPreviousStarIsClearedInSameMission() {
        MissionService missionService = new MissionService(
                missionSetRepository,
                missionSetProgressRepository,
                childActivityService
        );
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        List<MissionSet> missionSets = List.of(
                missionSet("S0104-1-1", missionId, "S0104", (short) 1, 1, 1),
                missionSet("S0104-1-2", missionId, "S0104", (short) 1, 1, 2),
                missionSet("S0104-2-1", missionId, "S0104", (short) 1, 2, 1),
                missionSet("S0104-3-1", missionId, "S0104", (short) 1, 3, 1)
        );
        when(missionSetRepository.findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc())
                .thenReturn(missionSets);
        when(missionSetProgressRepository.findAllByChildIdAndSetIdIn(eq(childId), anyCollection()))
                .thenReturn(List.of(progress(childId, "S0104-1-1", missionId, 1, 1)));

        MissionListResponse response = missionService.getMissions(childId);

        assertThat(response.stages().getFirst().missions()).singleElement().satisfies(mission -> {
            assertThat(mission.starLevels()).hasSize(3);
            assertThat(mission.starLevels().get(0).isPlayable()).isTrue();
            assertThat(mission.starLevels().get(1).isPlayable()).isTrue();
            assertThat(mission.starLevels().get(2).isPlayable()).isFalse();
        });
    }

    @Test
    void nextStageRequiresAllPreviousStageMissionsToClearStarOne() {
        MissionService missionService = new MissionService(
                missionSetRepository,
                missionSetProgressRepository,
                childActivityService
        );
        UUID childId = UUID.randomUUID();
        UUID stage2MissionId = UUID.randomUUID();
        UUID stage1Mission1 = UUID.randomUUID();
        UUID stage1Mission2 = UUID.randomUUID();
        UUID stage1Mission3 = UUID.randomUUID();
        UUID stage1Mission4 = UUID.randomUUID();
        UUID stage1Mission5 = UUID.randomUUID();
        List<MissionSet> missionSets = List.of(
                missionSet("S0101-1-1", stage1Mission1, "S0101", (short) 1, 1, 1),
                missionSet("S0102-1-1", stage1Mission2, "S0102", (short) 1, 1, 2),
                missionSet("S0103-1-1", stage1Mission3, "S0103", (short) 1, 1, 3),
                missionSet("S0104-1-1", stage1Mission4, "S0104", (short) 1, 1, 4),
                missionSet("S0105-2-1", stage1Mission5, "S0105", (short) 1, 2, 5),
                missionSet("S0201-1-1", stage2MissionId, "S0201", (short) 2, 1, 6)
        );
        when(missionSetRepository.findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc())
                .thenReturn(missionSets);
        when(missionSetProgressRepository.findAllByChildIdAndSetIdIn(eq(childId), anyCollection()))
                .thenReturn(List.of(
                        progress(childId, "S0101-1-1", stage1Mission1, 1, 1),
                        progress(childId, "S0102-1-1", stage1Mission2, 1, 1),
                        progress(childId, "S0103-1-1", stage1Mission3, 1, 1),
                        progress(childId, "S0104-1-1", stage1Mission4, 1, 1),
                        progress(childId, "S0105-2-1", stage1Mission5, 1, 2)
                ));

        MissionListResponse response = missionService.getMissions(childId);

        assertThat(response.stages().get(1).missions()).singleElement().satisfies(mission -> {
            assertThat(mission.missionCode()).isEqualTo("S0201");
            assertThat(mission.isUnlocked()).isFalse();
            assertThat(mission.starLevels().getFirst().isPlayable()).isFalse();
        });
    }

    private MissionSet missionSet(String setId, UUID missionId, String missionCode, short stage, int starLevel, int displayOrder) {
        MissionSet missionSet = org.mockito.Mockito.mock(MissionSet.class);
        when(missionSet.getSetId()).thenReturn(setId);
        when(missionSet.getMissionId()).thenReturn(missionId);
        lenient().when(missionSet.getMissionCode()).thenReturn(missionCode);
        when(missionSet.getStage()).thenReturn(stage);
        when(missionSet.getStarLevel()).thenReturn(starLevel);
        lenient().when(missionSet.getDisplayOrder()).thenReturn(displayOrder);
        lenient().when(missionSet.getTitle()).thenReturn("Mission " + missionCode);
        lenient().when(missionSet.getDescription()).thenReturn("Description");
        return missionSet;
    }

    private MissionSetProgress progress(UUID childId, String setId, UUID missionId, int stage, int starLevel) {
        return MissionSetProgress.create(childId, setId, missionId, stage, starLevel, 1, UUID.randomUUID(), 100, 10);
    }
}
