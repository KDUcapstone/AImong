package com.aimong.backend.domain.mission.service.question;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.argThat;
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
    void enqueuesAndProcessesSoftThresholdMission() {
        AsyncMissionRefillService service = new AsyncMissionRefillService(
                metricsCollector,
                dynamicQuestionGenerationPort,
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 30000L, 10, 2),
                new MissionQuestionProperties(10, 30, true)
        );

        UUID missionId = UUID.randomUUID();
        when(metricsCollector.collect(missionId))
                .thenReturn(new QuestionPoolMetricsCollector.QuestionPoolMetrics(missionId, 30, 2, 18, 8, 4, List.of((short) 1, (short) 2)))
                .thenReturn(new QuestionPoolMetricsCollector.QuestionPoolMetrics(missionId, 30, 2, 18, 8, 4, List.of((short) 1, (short) 2)));

        service.enqueueIfNeeded(missionId);
        service.processQueuedMissions();

        verify(dynamicQuestionGenerationPort).generateQuestions(
                org.mockito.ArgumentMatchers.eq(missionId),
                argThat(details -> details.lowMissing() == 10 && details.mediumMissing() == 0 && details.highMissing() == 0),
                org.mockito.ArgumentMatchers.eq(new UUID(0L, 0L)),
                org.mockito.ArgumentMatchers.eq(false)
        );
    }

    @Test
    void processesCriticalMissionImmediatelyAtHardThreshold() {
        AsyncMissionRefillService service = new AsyncMissionRefillService(
                metricsCollector,
                dynamicQuestionGenerationPort,
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 30000L, 10, 2),
                new MissionQuestionProperties(10, 30, true)
        );

        UUID missionId = UUID.randomUUID();
        when(metricsCollector.collect(missionId))
                .thenReturn(new QuestionPoolMetricsCollector.QuestionPoolMetrics(missionId, 18, 1, 10, 6, 2, List.of((short) 1)))
                .thenReturn(new QuestionPoolMetricsCollector.QuestionPoolMetrics(missionId, 18, 1, 10, 6, 2, List.of((short) 1)));

        service.enqueueIfNeeded(missionId);

        verify(dynamicQuestionGenerationPort).generateQuestions(
                org.mockito.ArgumentMatchers.eq(missionId),
                argThat(details -> details.lowMissing() == 10 && details.mediumMissing() == 0 && details.highMissing() == 0),
                org.mockito.ArgumentMatchers.eq(new UUID(0L, 0L)),
                org.mockito.ArgumentMatchers.eq(false)
        );
    }

    @Test
    void doesNotEnqueueWhenDynamicGenerationDisabled() {
        AsyncMissionRefillService service = new AsyncMissionRefillService(
                metricsCollector,
                dynamicQuestionGenerationPort,
                new QuestionGenerationProperties(60, 10, 36, 18, 10, 30000L, 10, 2),
                new MissionQuestionProperties(10, 30, false)
        );

        service.enqueueIfNeeded(UUID.randomUUID());
        service.processQueuedMissions();

        verify(metricsCollector, never()).collect(org.mockito.ArgumentMatchers.any());
        verify(dynamicQuestionGenerationPort, never()).generateQuestions(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean()
        );
    }
}
