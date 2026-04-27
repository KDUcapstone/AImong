package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.service.QuestionPoolMetricsCollector;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncMissionRefillService {

    private static final Logger log = LoggerFactory.getLogger(AsyncMissionRefillService.class);
    private static final UUID SYSTEM_CHILD_ID = new UUID(0L, 0L);

    private final QuestionPoolMetricsCollector metricsCollector;
    private final DynamicQuestionGenerationPort dynamicQuestionGenerationPort;
    private final QuestionGenerationProperties generationProperties;
    private final MissionQuestionProperties missionQuestionProperties;

    private final Set<UUID> queuedMissionIds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> processingMissionIds = ConcurrentHashMap.newKeySet();

    public void enqueueIfNeeded(UUID missionId) {
        if (!missionQuestionProperties.dynamicGenerationEnabled()) {
            return;
        }

        QuestionPoolMetricsCollector.QuestionPoolMetrics metrics = metricsCollector.collect(missionId);
        if (metrics.activeCount() <= generationProperties.hardRefillTrigger()) {
            log.info(
                    "mission-refill critical missionId={} activeCount={} intactPackCount={}",
                    missionId,
                    metrics.activeCount(),
                    metrics.intactPackCount()
            );
            refillImmediately(missionId);
            return;
        }
        if (metrics.activeCount() <= generationProperties.softRefillTrigger()) {
            queuedMissionIds.add(missionId);
            log.info(
                    "mission-refill enqueued missionId={} activeCount={} intactPackCount={}",
                    missionId,
                    metrics.activeCount(),
                    metrics.intactPackCount()
            );
        }
    }

    private void refillImmediately(UUID missionId) {
        if (!processingMissionIds.add(missionId)) {
            queuedMissionIds.add(missionId);
            return;
        }
        try {
            queuedMissionIds.remove(missionId);
            refillMission(missionId);
        } finally {
            processingMissionIds.remove(missionId);
        }
    }

    @Scheduled(fixedDelayString = "${aimong.mission.generation.async-refill-fixed-delay-ms:30000}")
    public void processQueuedMissions() {
        if (!missionQuestionProperties.dynamicGenerationEnabled()) {
            return;
        }

        for (UUID missionId : Set.copyOf(queuedMissionIds)) {
            if (!processingMissionIds.add(missionId)) {
                continue;
            }
            try {
                queuedMissionIds.remove(missionId);
                refillMission(missionId);
            } finally {
                processingMissionIds.remove(missionId);
            }
        }
    }

    void refillMission(UUID missionId) {
        QuestionPoolMetricsCollector.QuestionPoolMetrics metrics = metricsCollector.collect(missionId);
        int shortage = generationProperties.targetPoolPerMission() - (int) metrics.activeCount();
        if (shortage <= 0) {
            return;
        }

        int batchSize = Math.min(shortage, generationProperties.asyncGenerateBatch());
        int lowShortage = Math.max(0, 30 - (int) metrics.lowBandCount());
        int mediumShortage = Math.max(0, 20 - (int) metrics.mediumBandCount());
        int highShortage = Math.max(0, 10 - (int) metrics.highBandCount());
        int lowRequest = Math.min(lowShortage, batchSize);
        int remaining = batchSize - lowRequest;
        int mediumRequest = Math.min(mediumShortage, remaining);
        remaining -= mediumRequest;
        int highRequest = Math.min(highShortage, remaining);
        dynamicQuestionGenerationPort.generateQuestions(
                missionId,
                new RecompositionSelector.ShortageDetails(
                        lowRequest,
                        mediumRequest,
                        highRequest,
                        0,
                        "ASYNC_POOL_REFILL",
                        new RecompositionSelector.CandidatePoolCounts(
                                (int) metrics.activeCount(),
                                (int) metrics.lowBandCount(),
                                (int) metrics.mediumBandCount(),
                                (int) metrics.highBandCount()
                        )
                ),
                SYSTEM_CHILD_ID,
                false
        );

        log.info(
                "mission-refill processed missionId={} activeCount={} requestedBatch={}",
                missionId,
                metrics.activeCount(),
                batchSize
        );
    }

    boolean isQueued(UUID missionId) {
        return queuedMissionIds.contains(missionId);
    }
}
