package com.aimong.backend.domain.mission.service.question;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.service.QuestionPoolMetricsCollector;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncMissionRefillServiceTest {

    @Mock
    private QuestionPoolMetricsCollector metricsCollector;

    @Mock
    private DynamicQuestionGenerationPort dynamicQuestionGenerationPort;

    @Test
    void enqueuesAndProcessesLowPoolMission() {
        AsyncMissionRefillService service = new AsyncMissionRefillService(
                metricsCollector,
                dynamicQuestionGenerationPort,
                new QuestionGenerationProperties(60, 6, 10, 36, 18, 10, 30000L, 10, 2),
                new MissionQuestionProperties(10, 30, true)
        );

        UUID missionId = UUID.randomUUID();
        when(metricsCollector.collect(missionId))
                .thenReturn(new QuestionPoolMetricsCollector.QuestionPoolMetrics(missionId, 18, 1, 10, 6, 2, List.of((short) 1)))
                .thenReturn(new QuestionPoolMetricsCollector.QuestionPoolMetrics(missionId, 18, 1, 10, 6, 2, List.of((short) 1)));

        service.enqueueIfNeeded(missionId);
        service.processQueuedMissions();

        verify(dynamicQuestionGenerationPort).generateQuestions(missionId, 10, new UUID(0L, 0L), false);
    }

    @Test
    void doesNotEnqueueWhenDynamicGenerationDisabled() {
        AsyncMissionRefillService service = new AsyncMissionRefillService(
                metricsCollector,
                dynamicQuestionGenerationPort,
                new QuestionGenerationProperties(60, 6, 10, 36, 18, 10, 30000L, 10, 2),
                new MissionQuestionProperties(10, 30, false)
        );

        service.enqueueIfNeeded(UUID.randomUUID());
        service.processQueuedMissions();

        verify(metricsCollector, never()).collect(org.mockito.ArgumentMatchers.any());
        verify(dynamicQuestionGenerationPort, never()).generateQuestions(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
