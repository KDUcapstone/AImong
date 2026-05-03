package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MissionCodeResolverTest {

    @Mock
    private MissionRepository missionRepository;

    @Test
    void fallsBackToStageOrderWhenMissionCodeAndTitleMatchAreUnavailable() {
        MissionCodeResolver resolver = new MissionCodeResolver(
                missionRepository,
                new KerisCurriculumRegistry()
        );

        Mission mission1 = mission((short) 1, null, "broken-1");
        Mission mission2 = mission((short) 1, null, "broken-2");
        Mission mission3 = mission((short) 1, null, "broken-3");
        Mission mission4 = mission((short) 1, null, "broken-4");
        Mission mission5 = mission((short) 1, null, "broken-5");

        when(missionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc())
                .thenReturn(List.of(mission1, mission2, mission3, mission4, mission5));

        assertThat(resolver.resolve(mission1)).contains("S0101");
        assertThat(resolver.resolve(mission2)).contains("S0102");
        assertThat(resolver.resolve(mission5)).contains("S0105");
    }

    private Mission mission(short stage, String missionCode, String title) {
        Mission mission = org.mockito.Mockito.mock(Mission.class);
        when(mission.getId()).thenReturn(UUID.randomUUID());
        when(mission.getStage()).thenReturn(stage);
        lenient().when(mission.getMissionCode()).thenReturn(missionCode);
        lenient().when(mission.getTitle()).thenReturn(title);
        return mission;
    }
}
