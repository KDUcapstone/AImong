package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitialSeedGenerationJobTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private QuestionGenerationService questionGenerationService;

    @Mock
    private GeneratedQuestionPersistenceService persistenceService;

    @Mock
    private MissionCodeResolver missionCodeResolver;

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Test
    void seedManifestMatchesExactNineHundredSixtyQuota() {
        InitialSeedGenerationJob job = job();
        List<Mission> missions = activeMissions();

        when(missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc()).thenReturn(missions);
        for (Mission mission : missions) {
            String missionCode = mission.getMissionCode();
            when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of(missionCode));
        }
        when(questionGenerationService.generateValidatedCandidates(any())).thenAnswer(invocation -> {
            QuestionGenerationService.QuestionGenerationRequest request = invocation.getArgument(0);
            return new QuestionGenerationService.GenerationBatchResult(
                    new ModelRoutingPolicy.RoutingDecision("gpt-5-mini", false),
                    List.of(candidate(request)),
                    List.of()
            );
        });

        InitialSeedGenerationJob.SeedGenerationManifest manifest = job.generateInitialSeed(false);

        assertThat(manifest.status()).isEqualTo(InitialSeedGenerationJob.SeedGenerationStatus.COMPLETE);
        assertThat(manifest.totalMissions()).isEqualTo(16);
        assertThat(manifest.totalPacks()).isEqualTo(96);
        assertThat(manifest.totalQuestions()).isEqualTo(960);
        assertThat(manifest.totalTypeCounts()).containsEntry(QuestionType.OX, 192);
        assertThat(manifest.totalTypeCounts()).containsEntry(QuestionType.MULTIPLE, 288);
        assertThat(manifest.totalTypeCounts()).containsEntry(QuestionType.FILL, 192);
        assertThat(manifest.totalTypeCounts()).containsEntry(QuestionType.SITUATION, 288);
        assertThat(manifest.totalDifficultyCounts()).containsEntry(DifficultyBand.LOW, 480);
        assertThat(manifest.totalDifficultyCounts()).containsEntry(DifficultyBand.MEDIUM, 320);
        assertThat(manifest.totalDifficultyCounts()).containsEntry(DifficultyBand.HIGH, 160);
        assertThat(manifest.missions()).allSatisfy(mission -> {
            assertThat(mission.status()).isEqualTo(InitialSeedGenerationJob.SeedGenerationStatus.COMPLETE);
            assertThat(mission.packCount()).isEqualTo(6);
            assertThat(mission.questionCount()).isEqualTo(60);
            assertThat(mission.typeCounts()).containsEntry(QuestionType.OX, 12);
            assertThat(mission.typeCounts()).containsEntry(QuestionType.MULTIPLE, 18);
            assertThat(mission.typeCounts()).containsEntry(QuestionType.FILL, 12);
            assertThat(mission.typeCounts()).containsEntry(QuestionType.SITUATION, 18);
            assertThat(mission.difficultyCounts()).containsEntry(DifficultyBand.LOW, 30);
            assertThat(mission.difficultyCounts()).containsEntry(DifficultyBand.MEDIUM, 20);
            assertThat(mission.difficultyCounts()).containsEntry(DifficultyBand.HIGH, 10);
            assertThat(mission.packs()).allSatisfy(pack -> {
                assertThat(pack.status()).isEqualTo(InitialSeedGenerationJob.SeedGenerationStatus.COMPLETE);
                assertThat(pack.questionCount()).isEqualTo(10);
                assertThat(pack.typeCounts()).containsEntry(QuestionType.OX, 2);
                assertThat(pack.typeCounts()).containsEntry(QuestionType.MULTIPLE, 3);
                assertThat(pack.typeCounts()).containsEntry(QuestionType.FILL, 2);
                assertThat(pack.typeCounts()).containsEntry(QuestionType.SITUATION, 3);
            });
        });
    }

    @Test
    void retryAndCompletionLoopRecoverFromRejectedSlot() {
        InitialSeedGenerationJob job = job();
        Mission mission = activeMissions().getFirst();
        String missionCode = mission.getMissionCode();

        when(missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc()).thenReturn(List.of(mission));
        when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of(missionCode));

        Deque<Boolean> results = new ArrayDeque<>();
        results.add(false);
        for (int index = 0; index < 60; index++) {
            results.add(true);
        }

        when(questionGenerationService.generateValidatedCandidates(any())).thenAnswer(invocation -> {
            QuestionGenerationService.QuestionGenerationRequest request = invocation.getArgument(0);
            boolean accepted = results.removeFirst();
            return new QuestionGenerationService.GenerationBatchResult(
                    new ModelRoutingPolicy.RoutingDecision("gpt-5-mini", false),
                    accepted ? List.of(candidate(request)) : List.of(),
                    accepted ? List.of() : List.of(rejectedCandidate(request))
            );
        });

        InitialSeedGenerationJob.SeedGenerationManifest manifest = job.generateInitialSeed(false);

        assertThat(manifest.status()).isEqualTo(InitialSeedGenerationJob.SeedGenerationStatus.COMPLETE);
        assertThat(manifest.totalQuestions()).isEqualTo(60);
        assertThat(results).isEmpty();
    }

    @Test
    void incompleteStatusIsNotReportedAsSuccess() {
        InitialSeedGenerationJob job = job();
        Mission mission = activeMissions().getFirst();
        String missionCode = mission.getMissionCode();

        when(missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc()).thenReturn(List.of(mission));
        when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of(missionCode));
        when(questionGenerationService.generateValidatedCandidates(any())).thenReturn(
                new QuestionGenerationService.GenerationBatchResult(
                        new ModelRoutingPolicy.RoutingDecision("gpt-5-mini", false),
                        List.of(),
                        List.of(rejectedCandidate(new QuestionGenerationService.QuestionGenerationRequest(
                                missionCode, 1, DifficultyBand.LOW, QuestionType.OX, 1, 1, 0, false, false, false, false, List.of(), List.of(), List.of()
                        )))
                )
        );

        InitialSeedGenerationJob.SeedGenerationManifest manifest = job.generateInitialSeed(false);

        assertThat(manifest.status()).isEqualTo(InitialSeedGenerationJob.SeedGenerationStatus.INCOMPLETE);
        assertThat(manifest.totalQuestions()).isZero();
        assertThat(manifest.missions().getFirst().missingSlots()).isNotEmpty();
    }

    @Test
    void idempotentPersistedMissionSkipsRegenerationWhenPoolAlreadyComplete() {
        InitialSeedGenerationJob job = job();
        Mission mission = activeMissions().getFirst();
        String missionCode = mission.getMissionCode();

        when(missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc()).thenReturn(List.of(mission));
        when(missionCodeResolver.resolve(mission)).thenReturn(Optional.of(missionCode));
        when(questionBankRepository.countByMissionIdAndIsActiveTrue(mission.getId())).thenReturn(60L);

        InitialSeedGenerationJob.SeedGenerationManifest manifest = job.generateInitialSeed(true);

        assertThat(manifest.status()).isEqualTo(InitialSeedGenerationJob.SeedGenerationStatus.COMPLETE);
        assertThat(manifest.missions().getFirst().message()).isEqualTo("MISSION_ALREADY_COMPLETE");
        assertThat(manifest.totalQuestions()).isZero();
    }

    private InitialSeedGenerationJob job() {
        return new InitialSeedGenerationJob(
                missionRepository,
                questionGenerationService,
                persistenceService,
                new PackQuotaPlanner(new QuestionGenerationProperties(60, 6, 10, 36, 18, 10, 30000L, 10, 2)),
                new QuestionGenerationProperties(60, 6, 10, 36, 18, 10, 30000L, 10, 2),
                missionCodeResolver,
                questionBankRepository
        );
    }

    private List<Mission> activeMissions() {
        List<Mission> missions = new ArrayList<>();
        missions.addAll(createMissions(1, "S01", 5));
        missions.addAll(createMissions(2, "S02", 6));
        missions.addAll(createMissions(3, "S03", 5));
        return missions;
    }

    private List<Mission> createMissions(int stage, String prefix, int count) {
        List<Mission> missions = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            Mission mission = org.mockito.Mockito.mock(Mission.class);
            lenient().when(mission.getId()).thenReturn(UUID.randomUUID());
            lenient().when(mission.getStage()).thenReturn((short) stage);
            lenient().when(mission.getMissionCode()).thenReturn(prefix + String.format("%02d", index));
            missions.add(mission);
        }
        return missions;
    }

    private StructuredQuestionSchema candidate(QuestionGenerationService.QuestionGenerationRequest request) {
        return new StructuredQuestionSchema(
                request.missionCode(),
                request.packNo(),
                request.difficultyBand(),
                request.desiredType(),
                request.missionCode() + "-" + request.packNo() + "-" + request.desiredType() + "-" + request.difficultyBand() + "-" + request.validationFailureCount(),
                optionsFor(request.desiredType()),
                answerFor(request.desiredType()),
                "설명은 두 문장 이하입니다.",
                tagsFor(request.missionCode()),
                "KERIS-REF",
                request.numericDifficulty()
        );
    }

    private QuestionGenerationService.RejectedCandidate rejectedCandidate(QuestionGenerationService.QuestionGenerationRequest request) {
        return new QuestionGenerationService.RejectedCandidate(
                candidate(request),
                new QuestionValidationReport(
                        false,
                        ValidationDecision.RETRY,
                        List.of("retry"),
                        List.of(),
                        new QuestionValidationScores(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                        null,
                        List.of("repair")
                )
        );
    }

    private List<String> optionsFor(QuestionType type) {
        return switch (type) {
            case OX -> null;
            case MULTIPLE -> List.of("A", "B", "C", "D");
            case FILL -> List.of("A", "B", "C", "D");
            case SITUATION -> List.of("A", "B", "C");
        };
    }

    private Object answerFor(QuestionType type) {
        return switch (type) {
            case OX -> Boolean.TRUE;
            case MULTIPLE -> 0;
            case FILL -> List.of(0);
            case SITUATION -> 0;
        };
    }

    private List<String> tagsFor(String missionCode) {
        if (missionCode.startsWith("S01")) {
            return List.of("FACT", "VERIFICATION");
        }
        if (missionCode.startsWith("S02")) {
            return List.of("PROMPT", "SAFETY");
        }
        return List.of("VERIFICATION", "FACT");
    }
}
