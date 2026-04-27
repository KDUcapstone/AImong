package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.SeedJobProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialSeedGenerationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialSeedGenerationRunner.class);

    private final InitialSeedGenerationJob initialSeedGenerationJob;
    private final SeedJobProperties seedJobProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedJobProperties.autoRun()) {
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
    }
}
