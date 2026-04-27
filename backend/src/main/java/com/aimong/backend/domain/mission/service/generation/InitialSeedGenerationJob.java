package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitialSeedGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(InitialSeedGenerationJob.class);

    private final MissionRepository missionRepository;
    private final QuestionGenerationService questionGenerationService;
    private final GeneratedQuestionPersistenceService persistenceService;
    private final PackQuotaPlanner packQuotaPlanner;
    private final QuestionGenerationProperties generationProperties;

    public SeedGenerationManifest generateInitialSeed(boolean persist) {
        List<Mission> missions = missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc();
        List<MissionSeedResult> missionResults = new ArrayList<>();

        for (Mission mission : missions) {
            missionResults.add(generateForMission(mission, persist));
        }

        SeedGenerationManifest manifest = SeedGenerationManifest.from(missionResults);
        log.info(
                "initial-seed manifest totalMissions={} totalQuestions={} totalPacks={}",
                manifest.totalMissions(),
                manifest.totalQuestions(),
                manifest.totalPacks()
        );
        return manifest;
    }

    private MissionSeedResult generateForMission(Mission mission, boolean persist) {
        PackQuotaPlanner.MissionQuota missionQuota = packQuotaPlanner.planForMission();
        List<PackSeedResult> packResults = new ArrayList<>();
        List<String> knownPrompts = new ArrayList<>();

        for (PackQuotaPlanner.PackQuota packQuota : missionQuota.packs()) {
            packResults.add(generateForPack(mission, packQuota, knownPrompts, persist));
        }

        return MissionSeedResult.from(mission, packResults);
    }

    private PackSeedResult generateForPack(
            Mission mission,
            PackQuotaPlanner.PackQuota packQuota,
            List<String> knownPrompts,
            boolean persist
    ) {
        List<StructuredQuestionSchema> accepted = new ArrayList<>();
        List<QuestionGenerationService.RejectedCandidate> rejected = new ArrayList<>();

        for (DifficultySlot slot : expandDifficultySlots(packQuota)) {
            QuestionGenerationService.GenerationBatchResult batch = questionGenerationService.generateValidatedCandidates(
                    new QuestionGenerationService.QuestionGenerationRequest(
                            mission.getMissionCode(),
                            packQuota.packNo(),
                            slot.difficultyBand(),
                            slot.questionType(),
                            1,
                            inferNumericDifficulty(mission.getStage(), slot.difficultyBand()),
                            0,
                            false,
                            false,
                            false,
                            false,
                            List.copyOf(knownPrompts),
                            List.of()
                    )
            );

            if (!batch.accepted().isEmpty()) {
                StructuredQuestionSchema candidate = batch.accepted().getFirst();
                accepted.add(candidate);
                knownPrompts.add(candidate.question());
            }
            rejected.addAll(batch.rejected());
        }

        List<QuestionBank> persisted = persist
                ? persistenceService.persistCandidates(mission.getId(), accepted, GenerationPhase.PREGENERATED, "GPT")
                : List.of();

        return PackSeedResult.from(packQuota.packNo(), accepted, rejected, persisted.size());
    }

    private List<DifficultySlot> expandDifficultySlots(PackQuotaPlanner.PackQuota packQuota) {
        List<QuestionType> typeSlots = expandTypeSlots(packQuota.typeQuota());
        List<com.aimong.backend.domain.mission.entity.DifficultyBand> bandSlots = expandBandSlots(packQuota.difficultyQuota());

        List<DifficultySlot> slots = new ArrayList<>();
        for (int index = 0; index < generationProperties.questionsPerPack(); index++) {
            slots.add(new DifficultySlot(typeSlots.get(index), bandSlots.get(index)));
        }
        return slots;
    }

    private List<QuestionType> expandTypeSlots(Map<QuestionType, Integer> typeQuota) {
        List<QuestionType> slots = new ArrayList<>();
        QuestionType[] order = {
                QuestionType.OX,
                QuestionType.MULTIPLE,
                QuestionType.FILL,
                QuestionType.SITUATION
        };
        for (QuestionType type : order) {
            for (int count = 0; count < typeQuota.getOrDefault(type, 0); count++) {
                slots.add(type);
            }
        }
        return slots;
    }

    private List<com.aimong.backend.domain.mission.entity.DifficultyBand> expandBandSlots(
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> difficultyQuota
    ) {
        List<com.aimong.backend.domain.mission.entity.DifficultyBand> slots = new ArrayList<>();
        com.aimong.backend.domain.mission.entity.DifficultyBand[] order = {
                com.aimong.backend.domain.mission.entity.DifficultyBand.LOW,
                com.aimong.backend.domain.mission.entity.DifficultyBand.MEDIUM,
                com.aimong.backend.domain.mission.entity.DifficultyBand.HIGH
        };
        for (com.aimong.backend.domain.mission.entity.DifficultyBand band : order) {
            for (int count = 0; count < difficultyQuota.getOrDefault(band, 0); count++) {
                slots.add(band);
            }
        }
        return slots;
    }

    private int inferNumericDifficulty(short stage, com.aimong.backend.domain.mission.entity.DifficultyBand difficultyBand) {
        return switch (stage) {
            case 1 -> difficultyBand == com.aimong.backend.domain.mission.entity.DifficultyBand.LOW ? 1 : 2;
            case 2 -> difficultyBand == com.aimong.backend.domain.mission.entity.DifficultyBand.LOW ? 2 : 3;
            case 3 -> difficultyBand == com.aimong.backend.domain.mission.entity.DifficultyBand.LOW ? 3 : 4;
            default -> 1;
        };
    }

    private record DifficultySlot(
            QuestionType questionType,
            com.aimong.backend.domain.mission.entity.DifficultyBand difficultyBand
    ) {
    }

    public record SeedGenerationManifest(
            int totalMissions,
            int totalPacks,
            int totalQuestions,
            Map<QuestionType, Integer> totalTypeCounts,
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> totalDifficultyCounts,
            List<MissionSeedResult> missions
    ) {
        static SeedGenerationManifest from(List<MissionSeedResult> missionResults) {
            Map<QuestionType, Integer> typeCounts = new EnumMap<>(QuestionType.class);
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> difficultyCounts =
                    new EnumMap<>(com.aimong.backend.domain.mission.entity.DifficultyBand.class);

            int totalPacks = 0;
            int totalQuestions = 0;
            for (MissionSeedResult missionResult : missionResults) {
                totalPacks += missionResult.packCount();
                totalQuestions += missionResult.questionCount();
                missionResult.typeCounts().forEach((type, count) -> typeCounts.merge(type, count, Integer::sum));
                missionResult.difficultyCounts().forEach((band, count) -> difficultyCounts.merge(band, count, Integer::sum));
            }

            return new SeedGenerationManifest(
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
            int packCount,
            int questionCount,
            Map<QuestionType, Integer> typeCounts,
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> difficultyCounts,
            List<PackSeedResult> packs
    ) {
        static MissionSeedResult from(Mission mission, List<PackSeedResult> packResults) {
            Map<QuestionType, Integer> typeCounts = new EnumMap<>(QuestionType.class);
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> difficultyCounts =
                    new EnumMap<>(com.aimong.backend.domain.mission.entity.DifficultyBand.class);
            int questionCount = 0;
            for (PackSeedResult packResult : packResults) {
                questionCount += packResult.questionCount();
                packResult.typeCounts().forEach((type, count) -> typeCounts.merge(type, count, Integer::sum));
                packResult.difficultyCounts().forEach((band, count) -> difficultyCounts.merge(band, count, Integer::sum));
            }

            return new MissionSeedResult(
                    mission.getId(),
                    mission.getMissionCode(),
                    packResults.size(),
                    questionCount,
                    typeCounts,
                    difficultyCounts,
                    packResults
            );
        }
    }

    public record PackSeedResult(
            int packNo,
            int questionCount,
            int persistedCount,
            Map<QuestionType, Integer> typeCounts,
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> difficultyCounts,
            int rejectedCount
    ) {
        static PackSeedResult from(
                int packNo,
                List<StructuredQuestionSchema> accepted,
                List<QuestionGenerationService.RejectedCandidate> rejected,
                int persistedCount
        ) {
            Map<QuestionType, Integer> typeCounts = new LinkedHashMap<>();
            Map<com.aimong.backend.domain.mission.entity.DifficultyBand, Integer> difficultyCounts = new LinkedHashMap<>();
            for (StructuredQuestionSchema question : accepted) {
                typeCounts.merge(question.type(), 1, Integer::sum);
                difficultyCounts.merge(question.difficultyBand(), 1, Integer::sum);
            }
            return new PackSeedResult(
                    packNo,
                    accepted.size(),
                    persistedCount,
                    typeCounts,
                    difficultyCounts,
                    rejected.size()
            );
        }
    }
}
