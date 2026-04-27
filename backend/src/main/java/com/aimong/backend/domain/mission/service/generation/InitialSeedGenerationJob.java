package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class InitialSeedGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(InitialSeedGenerationJob.class);
    private static final Set<UUID> PROCESSING_MISSION_IDS = ConcurrentHashMap.newKeySet();
    private static final int SLOT_RETRY_LIMIT = 3;
    private static final int BAND_RETRY_LIMIT = 1;
    private static final int MISSION_RETRY_LIMIT = 2;

    private final MissionRepository missionRepository;
    private final QuestionGenerationService questionGenerationService;
    private final GeneratedQuestionPersistenceService persistenceService;
    private final PackQuotaPlanner packQuotaPlanner;
    private final QuestionGenerationProperties generationProperties;
    private final MissionCodeResolver missionCodeResolver;
    private final QuestionBankRepository questionBankRepository;

    public SeedGenerationManifest generateInitialSeed(boolean persist) {
        List<Mission> missions = missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc();
        List<MissionSeedResult> missionResults = new ArrayList<>();

        for (Mission mission : missions) {
            missionResults.add(generateForMissionSafely(mission, persist));
        }

        SeedGenerationManifest manifest = SeedGenerationManifest.from(missionResults);
        log.info(
                "initial-seed manifest status={} totalMissions={} totalQuestions={} totalPacks={}",
                manifest.status(),
                manifest.totalMissions(),
                manifest.totalQuestions(),
                manifest.totalPacks()
        );
        return manifest;
    }

    private MissionSeedResult generateForMissionSafely(Mission mission, boolean persist) {
        if (!PROCESSING_MISSION_IDS.add(mission.getId())) {
            return MissionSeedResult.failed(
                    mission.getId(),
                    mission.getMissionCode(),
                    "MISSION_LOCKED",
                    List.of()
            );
        }
        try {
            return generateForMission(mission, persist);
        } catch (RuntimeException exception) {
            log.error("initial-seed mission-failed missionId={} message={}", mission.getId(), exception.getMessage(), exception);
            return MissionSeedResult.failed(
                    mission.getId(),
                    mission.getMissionCode(),
                    exception.getMessage(),
                    List.of()
            );
        } finally {
            PROCESSING_MISSION_IDS.remove(mission.getId());
        }
    }

    private MissionSeedResult generateForMission(Mission mission, boolean persist) {
        String missionCode = missionCodeResolver.resolve(mission)
                .orElseThrow(() -> new IllegalArgumentException("Unknown missionCode for missionId: " + mission.getId()));
        if (persist && questionBankRepository.countByMissionIdAndIsActiveTrue(mission.getId()) >= generationProperties.targetPoolPerMission()) {
            return MissionSeedResult.complete(mission.getId(), missionCode, List.of(), "MISSION_ALREADY_COMPLETE");
        }

        PackQuotaPlanner.MissionQuota missionQuota = packQuotaPlanner.planForMission();
        List<String> knownPrompts = new ArrayList<>();
        List<PackSeedResult> packResults = new ArrayList<>();
        List<SlotFailure> missionFailures = new ArrayList<>();

        for (PackQuotaPlanner.PackQuota packQuota : missionQuota.packs()) {
            PackBuildState packState = generateForPack(mission, missionCode, packQuota, knownPrompts);
            packResults.add(toPackSeedResult(packQuota.packNo(), packState, persist, mission.getId()));
            missionFailures.addAll(packState.failures());
        }

        SeedGenerationStatus status = missionFailures.isEmpty() ? SeedGenerationStatus.COMPLETE : SeedGenerationStatus.INCOMPLETE;
        return MissionSeedResult.from(mission, missionCode, status, packResults, missionFailures, null);
    }

    private PackBuildState generateForPack(
            Mission mission,
            String missionCode,
            PackQuotaPlanner.PackQuota packQuota,
            List<String> knownPrompts
    ) {
        List<StructuredQuestionSchema> accepted = new ArrayList<>();
        List<QuestionGenerationService.RejectedCandidate> rejected = new ArrayList<>();
        List<SlotFailure> failures = new ArrayList<>();
        List<DifficultySlot> pendingSlots = new ArrayList<>(expandDifficultySlots(packQuota));

        attemptSlots(mission, missionCode, packQuota.packNo(), knownPrompts, accepted, rejected, failures, pendingSlots, List.copyOf(pendingSlots), "slot", SLOT_RETRY_LIMIT);
        retryPendingByBand(mission, missionCode, packQuota.packNo(), knownPrompts, accepted, rejected, failures, pendingSlots);
        retryPendingByMission(mission, missionCode, packQuota.packNo(), knownPrompts, accepted, rejected, failures, pendingSlots);

        return new PackBuildState(accepted, rejected, failures);
    }

    private void retryPendingByBand(
            Mission mission,
            String missionCode,
            int packNo,
            List<String> knownPrompts,
            List<StructuredQuestionSchema> accepted,
            List<QuestionGenerationService.RejectedCandidate> rejected,
            List<SlotFailure> failures,
            List<DifficultySlot> pendingSlots
    ) {
        for (DifficultyBand band : List.of(DifficultyBand.LOW, DifficultyBand.MEDIUM, DifficultyBand.HIGH)) {
            List<DifficultySlot> bandSlots = pendingSlots.stream()
                    .filter(slot -> slot.difficultyBand() == band)
                    .toList();
            if (!bandSlots.isEmpty()) {
                attemptSlots(mission, missionCode, packNo, knownPrompts, accepted, rejected, failures, pendingSlots, bandSlots, "band", BAND_RETRY_LIMIT);
            }
        }
    }

    private void retryPendingByMission(
            Mission mission,
            String missionCode,
            int packNo,
            List<String> knownPrompts,
            List<StructuredQuestionSchema> accepted,
            List<QuestionGenerationService.RejectedCandidate> rejected,
            List<SlotFailure> failures,
            List<DifficultySlot> pendingSlots
        ) {
        for (int retry = 0; retry < MISSION_RETRY_LIMIT && !pendingSlots.isEmpty(); retry++) {
            attemptSlots(mission, missionCode, packNo, knownPrompts, accepted, rejected, failures, pendingSlots, List.copyOf(pendingSlots), "mission", 1);
        }
    }

    private void attemptSlots(
            Mission mission,
            String missionCode,
            int packNo,
            List<String> knownPrompts,
            List<StructuredQuestionSchema> accepted,
            List<QuestionGenerationService.RejectedCandidate> rejected,
            List<SlotFailure> failures,
            List<DifficultySlot> pendingSlots,
            List<DifficultySlot> candidateSlots,
            String phase,
            int retryLimit
    ) {
        for (DifficultySlot slot : candidateSlots) {
            if (!pendingSlots.contains(slot)) {
                continue;
            }
            boolean succeeded = false;
            QuestionGenerationRetryFeedback feedback = QuestionGenerationRetryFeedback.empty();
            for (int attempt = 1; attempt <= retryLimit; attempt++) {
                QuestionGenerationService.GenerationBatchResult batch = questionGenerationService.generateValidatedCandidates(
                        new QuestionGenerationService.QuestionGenerationRequest(
                                missionCode,
                                packNo,
                                slot.difficultyBand(),
                                slot.questionType(),
                                1,
                                inferNumericDifficulty(mission.getStage(), slot.difficultyBand()),
                                Math.max(0, attempt - 1),
                                feedback.wordingQualityWeak(),
                                feedback.highDuplicateRisk(),
                                feedback.optionQualityWeak(),
                                feedback.explanationQualityWeak(),
                                List.copyOf(knownPrompts),
                                List.of(),
                                feedback.repairHints()
                        )
                );
                rejected.addAll(batch.rejected());
                if (!batch.accepted().isEmpty()) {
                    StructuredQuestionSchema candidate = batch.accepted().getFirst();
                    accepted.add(candidate);
                    knownPrompts.add(candidate.question());
                    pendingSlots.remove(slot);
                    failures.removeIf(failure -> sameSlot(failure, slot, packNo));
                    succeeded = true;
                    break;
                }
                feedback = feedback.merge(QuestionGenerationRetryFeedback.fromRejected(batch.rejected()));
                log.warn(
                        "initial-seed retry phase={} missionCode={} packNo={} type={} band={} attempt={} rejected={}",
                        phase,
                        missionCode,
                        packNo,
                        slot.questionType(),
                        slot.difficultyBand(),
                        attempt,
                        batch.rejected().size()
                );
            }
            if (!succeeded) {
                failures.removeIf(failure -> sameSlot(failure, slot, packNo));
                failures.add(new SlotFailure(
                        packNo,
                        slot.slotNo(),
                        slot.questionType(),
                        slot.difficultyBand(),
                        phase.toUpperCase() + "_RETRY_EXHAUSTED",
                        retryLimit
                ));
            }
        }
    }

    private boolean sameSlot(SlotFailure failure, DifficultySlot slot, int packNo) {
        return failure.packNo() == packNo
                && failure.slotNo() == slot.slotNo()
                && failure.questionType() == slot.questionType()
                && failure.difficultyBand() == slot.difficultyBand();
    }

    private PackSeedResult toPackSeedResult(
            int packNo,
            PackBuildState packState,
            boolean persist,
            UUID missionId
    ) {
        List<QuestionBank> persisted = persist
                ? persistenceService.persistCandidates(missionId, packState.accepted(), GenerationPhase.PREGENERATED, "GPT")
                : List.of();
        return PackSeedResult.from(packNo, packState.accepted(), packState.rejected(), persisted.size(), packState.failures());
    }

    private List<DifficultySlot> expandDifficultySlots(PackQuotaPlanner.PackQuota packQuota) {
        List<QuestionType> typeSlots = expandTypeSlots(packQuota.typeQuota());
        List<DifficultyBand> bandSlots = expandBandSlots(packQuota.difficultyQuota());

        List<DifficultySlot> slots = new ArrayList<>();
        for (int index = 0; index < generationProperties.questionsPerPack(); index++) {
            slots.add(new DifficultySlot(index + 1, typeSlots.get(index), bandSlots.get(index)));
        }
        return slots;
    }

    private List<QuestionType> expandTypeSlots(Map<QuestionType, Integer> typeQuota) {
        List<QuestionType> slots = new ArrayList<>();
        for (QuestionType type : List.of(QuestionType.OX, QuestionType.MULTIPLE, QuestionType.FILL, QuestionType.SITUATION)) {
            for (int count = 0; count < typeQuota.getOrDefault(type, 0); count++) {
                slots.add(type);
            }
        }
        return slots;
    }

    private List<DifficultyBand> expandBandSlots(Map<DifficultyBand, Integer> difficultyQuota) {
        List<DifficultyBand> slots = new ArrayList<>();
        for (DifficultyBand band : List.of(DifficultyBand.LOW, DifficultyBand.MEDIUM, DifficultyBand.HIGH)) {
            for (int count = 0; count < difficultyQuota.getOrDefault(band, 0); count++) {
                slots.add(band);
            }
        }
        return slots;
    }

    private int inferNumericDifficulty(short stage, DifficultyBand difficultyBand) {
        return switch (stage) {
            case 1 -> difficultyBand == DifficultyBand.LOW ? 1 : 2;
            case 2 -> difficultyBand == DifficultyBand.LOW ? 2 : 3;
            case 3 -> difficultyBand == DifficultyBand.LOW ? 3 : 4;
            default -> 1;
        };
    }

    private record DifficultySlot(
            int slotNo,
            QuestionType questionType,
            DifficultyBand difficultyBand
    ) {
    }

    private record PackBuildState(
            List<StructuredQuestionSchema> accepted,
            List<QuestionGenerationService.RejectedCandidate> rejected,
            List<SlotFailure> failures
    ) {
    }

    public enum SeedGenerationStatus {
        COMPLETE,
        INCOMPLETE,
        FAILED
    }

    public record SeedGenerationManifest(
            SeedGenerationStatus status,
            int totalMissions,
            int totalPacks,
            int totalQuestions,
            Map<QuestionType, Integer> totalTypeCounts,
            Map<DifficultyBand, Integer> totalDifficultyCounts,
            List<MissionSeedResult> missions
    ) {
        static SeedGenerationManifest from(List<MissionSeedResult> missionResults) {
            Map<QuestionType, Integer> typeCounts = new EnumMap<>(QuestionType.class);
            Map<DifficultyBand, Integer> difficultyCounts = new EnumMap<>(DifficultyBand.class);
            int totalPacks = 0;
            int totalQuestions = 0;
            SeedGenerationStatus status = SeedGenerationStatus.COMPLETE;

            for (MissionSeedResult missionResult : missionResults) {
                totalPacks += missionResult.packCount();
                totalQuestions += missionResult.questionCount();
                missionResult.typeCounts().forEach((type, count) -> typeCounts.merge(type, count, Integer::sum));
                missionResult.difficultyCounts().forEach((band, count) -> difficultyCounts.merge(band, count, Integer::sum));
                if (missionResult.status() == SeedGenerationStatus.FAILED) {
                    status = SeedGenerationStatus.FAILED;
                } else if (missionResult.status() == SeedGenerationStatus.INCOMPLETE && status == SeedGenerationStatus.COMPLETE) {
                    status = SeedGenerationStatus.INCOMPLETE;
                }
            }

            return new SeedGenerationManifest(
                    status,
                    missionResults.size(),
                    totalPacks,
                    totalQuestions,
                    typeCounts,
                    difficultyCounts,
                    missionResults
            );
        }
    }

    public record MissionSeedResult(
            UUID missionId,
            String missionCode,
            SeedGenerationStatus status,
            int packCount,
            int questionCount,
            Map<QuestionType, Integer> typeCounts,
            Map<DifficultyBand, Integer> difficultyCounts,
            List<PackSeedResult> packs,
            List<SlotFailure> missingSlots,
            String message
    ) {
        static MissionSeedResult complete(UUID missionId, String missionCode, List<PackSeedResult> packResults, String message) {
            return from(missionId, missionCode, SeedGenerationStatus.COMPLETE, packResults, List.of(), message);
        }

        static MissionSeedResult failed(UUID missionId, String missionCode, String message, List<PackSeedResult> packResults) {
            return from(missionId, missionCode, SeedGenerationStatus.FAILED, packResults, List.of(), message);
        }

        static MissionSeedResult from(
                Mission mission,
                String missionCode,
                SeedGenerationStatus status,
                List<PackSeedResult> packResults,
                List<SlotFailure> missingSlots,
                String message
        ) {
            return from(mission.getId(), missionCode, status, packResults, missingSlots, message);
        }

        static MissionSeedResult from(
                UUID missionId,
                String missionCode,
                SeedGenerationStatus status,
                List<PackSeedResult> packResults,
                List<SlotFailure> missingSlots,
                String message
        ) {
            Map<QuestionType, Integer> typeCounts = new EnumMap<>(QuestionType.class);
            Map<DifficultyBand, Integer> difficultyCounts = new EnumMap<>(DifficultyBand.class);
            int questionCount = 0;
            for (PackSeedResult packResult : packResults) {
                questionCount += packResult.questionCount();
                packResult.typeCounts().forEach((type, count) -> typeCounts.merge(type, count, Integer::sum));
                packResult.difficultyCounts().forEach((band, count) -> difficultyCounts.merge(band, count, Integer::sum));
            }
            return new MissionSeedResult(
                    missionId,
                    missionCode,
                    status,
                    packResults.size(),
                    questionCount,
                    typeCounts,
                    difficultyCounts,
                    List.copyOf(packResults),
                    List.copyOf(missingSlots),
                    message
            );
        }
    }

    public record PackSeedResult(
            int packNo,
            SeedGenerationStatus status,
            int questionCount,
            int persistedCount,
            Map<QuestionType, Integer> typeCounts,
            Map<DifficultyBand, Integer> difficultyCounts,
            int rejectedCount,
            List<SlotFailure> missingSlots
    ) {
        static PackSeedResult from(
                int packNo,
                List<StructuredQuestionSchema> accepted,
                List<QuestionGenerationService.RejectedCandidate> rejected,
                int persistedCount,
                List<SlotFailure> failures
        ) {
            Map<QuestionType, Integer> typeCounts = new LinkedHashMap<>();
            Map<DifficultyBand, Integer> difficultyCounts = new LinkedHashMap<>();
            for (StructuredQuestionSchema question : accepted) {
                typeCounts.merge(question.type(), 1, Integer::sum);
                difficultyCounts.merge(question.difficultyBand(), 1, Integer::sum);
            }
            return new PackSeedResult(
                    packNo,
                    failures.isEmpty() ? SeedGenerationStatus.COMPLETE : SeedGenerationStatus.INCOMPLETE,
                    accepted.size(),
                    persistedCount,
                    typeCounts,
                    difficultyCounts,
                    rejected.size(),
                    List.copyOf(failures)
            );
        }
    }

    public record SlotFailure(
            int packNo,
            int slotNo,
            QuestionType questionType,
            DifficultyBand difficultyBand,
            String reason,
            int attempts
    ) {
    }
}
