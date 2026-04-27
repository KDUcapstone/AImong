package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.SeedJobProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "aimong.mission.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class InitialSeedGenerationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialSeedGenerationRunner.class);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final InitialSeedGenerationJob initialSeedGenerationJob;
    private final InitialSeedExecutionGuard initialSeedExecutionGuard;
    private final SeedJobProperties seedJobProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedJobProperties.autoRun()) {
            return;
        }
        if (!RUNNING.compareAndSet(false, true)) {
            log.warn("initial-seed runner skipped because another run is already in progress");
            return;
        }

        try {
            InitialSeedExecutionGuard.SeedExecutionStatus status = initialSeedExecutionGuard.inspect();
            if (seedJobProperties.persist() && status.shouldSkipPersistedSeed()) {
                log.info(
                        "initial-seed runner skipped because pool is already filled activeMissions={} activeQuestions={}",
                        status.activeMissionCount(),
                        status.activeQuestionCount()
                );
                return;
            }

            InitialSeedGenerationJob.SeedGenerationManifest manifest =
                    initialSeedGenerationJob.generateInitialSeed(seedJobProperties.persist());

            log.info(
                    "initial-seed runner completed autoRun={} persist={} totalMissions={} totalPacks={} totalQuestions={}",
                    seedJobProperties.autoRun(),
                    seedJobProperties.persist(),
                    manifest.totalMissions(),
                    manifest.totalPacks(),
                    manifest.totalQuestions()
            );
        } catch (Throwable exception) {
            log.error(
                    "initial-seed runner failed but application startup will continue autoRun={} persist={} message={}",
                    seedJobProperties.autoRun(),
                    seedJobProperties.persist(),
                    exception.getMessage(),
                    exception
            );
        } finally {
            RUNNING.set(false);
        }
    }
}
