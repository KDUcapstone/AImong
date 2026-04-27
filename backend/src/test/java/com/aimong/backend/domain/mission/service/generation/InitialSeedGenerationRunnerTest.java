package com.aimong.backend.domain.mission.service.generation;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.SeedJobProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class InitialSeedGenerationRunnerTest {

    @Mock
    private InitialSeedGenerationJob initialSeedGenerationJob;

    @Test
    void doesNothingWhenAutoRunDisabled() throws Exception {
        InitialSeedGenerationRunner runner = new InitialSeedGenerationRunner(
                initialSeedGenerationJob,
                new SeedJobProperties(false, false)
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(initialSeedGenerationJob, never()).generateInitialSeed(org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void runsSeedJobWithConfiguredPersistFlag() throws Exception {
        InitialSeedGenerationRunner runner = new InitialSeedGenerationRunner(
                initialSeedGenerationJob,
                new SeedJobProperties(true, true)
        );
        when(initialSeedGenerationJob.generateInitialSeed(true)).thenReturn(
                new InitialSeedGenerationJob.SeedGenerationManifest(
                        16,
                        96,
                        960,
                        Map.of(),
                        Map.of(),
                        List.of()
                )
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(initialSeedGenerationJob).generateInitialSeed(true);
    }
}
