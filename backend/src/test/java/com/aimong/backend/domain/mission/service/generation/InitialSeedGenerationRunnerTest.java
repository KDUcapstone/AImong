package com.aimong.backend.domain.mission.service.generation;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

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

    @Mock
    private InitialSeedExecutionGuard initialSeedExecutionGuard;

    @Test
    void doesNothingWhenAutoRunDisabled() throws Exception {
        InitialSeedGenerationRunner runner = new InitialSeedGenerationRunner(
                initialSeedGenerationJob,
                initialSeedExecutionGuard,
                new SeedJobProperties(false, false, false)
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(initialSeedGenerationJob, never()).generateInitialSeed(org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void runsSeedJobWithConfiguredPersistFlag() throws Exception {
        InitialSeedGenerationRunner runner = new InitialSeedGenerationRunner(
                initialSeedGenerationJob,
                initialSeedExecutionGuard,
                new SeedJobProperties(true, true, true)
        );
        when(initialSeedExecutionGuard.inspect()).thenReturn(
                new InitialSeedExecutionGuard.SeedExecutionStatus(16, 0, false, false)
        );
        when(initialSeedGenerationJob.generateInitialSeed(true)).thenReturn(
                new InitialSeedGenerationJob.SeedGenerationManifest(
                        InitialSeedGenerationJob.SeedGenerationStatus.COMPLETE,
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

    @Test
    void skipsPersistedSeedWhenTargetPoolAlreadyFilled() throws Exception {
        InitialSeedGenerationRunner runner = new InitialSeedGenerationRunner(
                initialSeedGenerationJob,
                initialSeedExecutionGuard,
                new SeedJobProperties(true, true, true)
        );
        when(initialSeedExecutionGuard.inspect()).thenReturn(
                new InitialSeedExecutionGuard.SeedExecutionStatus(16, 960, true, true)
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(initialSeedGenerationJob, never()).generateInitialSeed(org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void swallowsSeedJobFailureSoStartupCanContinue() throws Exception {
        InitialSeedGenerationRunner runner = new InitialSeedGenerationRunner(
                initialSeedGenerationJob,
                initialSeedExecutionGuard,
                new SeedJobProperties(true, true, false)
        );
        when(initialSeedExecutionGuard.inspect()).thenReturn(
                new InitialSeedExecutionGuard.SeedExecutionStatus(16, 0, false, false)
        );
        doThrow(new IllegalStateException("insufficient_quota")).when(initialSeedGenerationJob)
                .generateInitialSeed(false);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(initialSeedGenerationJob).generateInitialSeed(false);
    }
}
