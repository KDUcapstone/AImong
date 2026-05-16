package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissionService {

    private static final long STAGE_2_REQUIRED_STAGE_1_STAR1_CLEARS = 5L;
    private static final long STAGE_3_REQUIRED_STAGE_2_STAR1_CLEARS = 6L;

    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final MissionRepository legacyMissionRepository;
    private final MissionAttemptRepository legacyMissionAttemptRepository;
    private final ChildActivityService childActivityService;

    @Autowired
    public MissionService(
            MissionSetRepository missionSetRepository,
            MissionSetProgressRepository missionSetProgressRepository,
            ChildActivityService childActivityService
    ) {
        this.missionSetRepository = missionSetRepository;
        this.missionSetProgressRepository = missionSetProgressRepository;
        this.legacyMissionRepository = null;
        this.legacyMissionAttemptRepository = null;
        this.childActivityService = childActivityService;
    }

    public MissionService(
            MissionRepository missionRepository,
            MissionAttemptRepository missionAttemptRepository,
            ChildActivityService childActivityService
    ) {
        this.missionSetRepository = null;
        this.missionSetProgressRepository = null;
        this.legacyMissionRepository = missionRepository;
        this.legacyMissionAttemptRepository = missionAttemptRepository;
        this.childActivityService = childActivityService;
    }

    @Transactional(readOnly = true)
    public MissionListResponse getMissions(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        if (missionSetRepository == null || missionSetProgressRepository == null) {
            return getLegacyMissions(childId);
        }

        List<MissionSet> missionSets = missionSetRepository
                .findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc();
        Map<String, MissionSetProgress> progressBySetId = progressBySetId(childId, missionSets);
        StageProgressResponse stageProgress = stageProgress(missionSets, progressBySetId);

        List<MissionListResponse.StageResponse> stages = missionSets.stream()
                .collect(Collectors.groupingBy(
                        MissionSet::getStage,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> toStageResponse(entry.getKey(), entry.getValue(), progressBySetId, stageProgress))
                .toList();

        long completedSetCount = progressBySetId.size();
        long totalSetCount = missionSets.size();
        return new MissionListResponse(
                stages,
                new MissionListResponse.ProgressResponse(completedSetCount, totalSetCount)
        );
    }

    public boolean isValidStarLevel(int starLevel) {
        return starLevel >= 1 && starLevel <= 3;
    }

    public boolean isUnlocked(Mission mission, StageProgressResponse stageProgress) {
        return isStageUnlocked(mission.getStage(), stageProgress);
    }

    public boolean isUnlockedForChild(UUID childId, Mission mission, StageProgressResponse stageProgress) {
        if (mission.getStage() <= 3) {
            return isUnlocked(mission, stageProgress);
        }
        return true;
    }

    public StageProgressResponse stageProgressForLegacy(UUID childId) {
        if (missionSetRepository == null || missionSetProgressRepository == null) {
            return new StageProgressResponse(
                    legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 1),
                    legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 2),
                    legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)
            );
        }
        List<MissionSet> missionSets = missionSetRepository
                .findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc();
        return stageProgress(missionSets, progressBySetId(childId, missionSets));
    }

    public boolean isUnlocked(UUID childId, MissionSet missionSet) {
        List<MissionSet> missionSets = missionSetRepository
                .findAllByActiveTrueOrderByStageAscDisplayOrderAscStarLevelAscVariantNoAscSetIdAsc();
        StageProgressResponse stageProgress = stageProgress(missionSets, progressBySetId(childId, missionSets));
        return isStageUnlocked(missionSet.getStage(), stageProgress);
    }

    public boolean isStarLevelPlayable(UUID childId, UUID missionId, int starLevel) {
        if (!isValidStarLevel(starLevel)) {
            throw new AimongException(ErrorCode.INVALID_STAR_LEVEL);
        }
        List<MissionSet> missionSets = missionSetRepository
                .findAllByMissionIdAndActiveTrueOrderByStarLevelAscVariantNoAscSetIdAsc(missionId);
        if (missionSets.isEmpty()) {
            return false;
        }
        if (!isUnlocked(childId, missionSets.getFirst())) {
            return false;
        }
        Map<Integer, List<MissionSet>> setsByStarLevel = groupByStarLevel(missionSets);
        return isStarLevelUnlocked(starLevel, setsByStarLevel, progressBySetId(childId, missionSets));
    }

    public MissionSet resolvePlayableSet(UUID childId, UUID missionId) {
        return resolvePlayableSet(childId, missionId, 1);
    }

    public MissionSet resolvePlayableSet(UUID childId, UUID missionId, int starLevel) {
        if (!isValidStarLevel(starLevel)) {
            throw new AimongException(ErrorCode.INVALID_STAR_LEVEL);
        }
        List<MissionSet> candidates = missionSetRepository
                .findAllByMissionIdAndStarLevelAndActiveTrueOrderByVariantNoAscSetIdAsc(missionId, starLevel);
        if (candidates.isEmpty()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        if (!isStarLevelPlayable(childId, missionId, starLevel)) {
            throw new AimongException(ErrorCode.MISSION_SET_LOCKED);
        }
        List<MissionSet> unlocked = candidates.stream()
                .filter(set -> isUnlocked(childId, set))
                .toList();
        if (unlocked.isEmpty()) {
            throw new AimongException(ErrorCode.MISSION_SET_LOCKED);
        }
        List<MissionSet> incomplete = unlocked.stream()
                .filter(set -> !missionSetProgressRepository.existsByChildIdAndSetId(childId, set.getSetId()))
                .toList();
        List<MissionSet> pool = incomplete.isEmpty() ? unlocked : incomplete;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private MissionListResponse.StageResponse toStageResponse(
            short stage,
            List<MissionSet> missionSets,
            Map<String, MissionSetProgress> progressBySetId,
            StageProgressResponse stageProgress
    ) {
        List<MissionListResponse.MissionResponse> missions = missionSets.stream()
                .collect(Collectors.groupingBy(
                        MissionSet::getMissionId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(sets -> toMissionResponse(stage, sets, progressBySetId, stageProgress))
                .sorted(Comparator.comparing(MissionListResponse.MissionResponse::missionCode,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
        return new MissionListResponse.StageResponse(stage, "Stage " + stage, missions);
    }

    private MissionListResponse.MissionResponse toMissionResponse(
            short stage,
            List<MissionSet> missionSets,
            Map<String, MissionSetProgress> progressBySetId,
            StageProgressResponse stageProgress
    ) {
        MissionSet first = missionSets.stream()
                .min(Comparator.comparing(MissionSet::getDisplayOrder).thenComparing(MissionSet::getSetId))
                .orElseThrow();
        boolean missionUnlocked = isStageUnlocked(stage, stageProgress);
        Map<Integer, List<MissionSet>> setsByStarLevel = groupByStarLevel(missionSets);
        List<MissionListResponse.StarLevelResponse> starLevels = setsByStarLevel
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toStarLevelResponse(
                        entry.getKey(),
                        entry.getValue(),
                        setsByStarLevel,
                        progressBySetId,
                        missionUnlocked
                ))
                .toList();
        return new MissionListResponse.MissionResponse(
                first.getMissionId(),
                first.getMissionCode(),
                first.getTitle(),
                first.getDescription(),
                missionUnlocked,
                starLevels,
                stage
        );
    }

    private MissionListResponse.StarLevelResponse toStarLevelResponse(
            int starLevel,
            List<MissionSet> missionSets,
            Map<Integer, List<MissionSet>> setsByStarLevel,
            Map<String, MissionSetProgress> progressBySetId,
            boolean missionUnlocked
    ) {
        long completed = missionSets.stream()
                .filter(set -> progressBySetId.containsKey(set.getSetId()))
                .count();
        boolean playable = missionUnlocked && isStarLevelUnlocked(starLevel, setsByStarLevel, progressBySetId);
        return new MissionListResponse.StarLevelResponse(
                starLevel,
                MissionListResponse.labelForStar(starLevel),
                missionSets.size(),
                completed,
                playable,
                completed > 0
        );
    }

    private StageProgressResponse stageProgress(
            Collection<MissionSet> missionSets,
            Map<String, MissionSetProgress> progressBySetId
    ) {
        Map<Short, Long> completedMissionsByStage = missionSets.stream()
                .filter(set -> set.getStarLevel() == 1)
                .filter(set -> progressBySetId.containsKey(set.getSetId()))
                .collect(Collectors.groupingBy(
                        MissionSet::getStage,
                        Collectors.mapping(MissionSet::getMissionId, Collectors.collectingAndThen(
                                Collectors.toSet(),
                                set -> (long) set.size()
                        ))
                ));
        return new StageProgressResponse(
                completedMissionsByStage.getOrDefault((short) 1, 0L),
                completedMissionsByStage.getOrDefault((short) 2, 0L),
                completedMissionsByStage.getOrDefault((short) 3, 0L)
        );
    }

    private boolean isStageUnlocked(short stage, StageProgressResponse stageProgress) {
        return switch (stage) {
            case 1 -> true;
            case 2 -> stageProgress.stage1Completed() >= STAGE_2_REQUIRED_STAGE_1_STAR1_CLEARS;
            case 3 -> stageProgress.stage2Completed() >= STAGE_3_REQUIRED_STAGE_2_STAR1_CLEARS;
            default -> false;
        };
    }

    private Map<Integer, List<MissionSet>> groupByStarLevel(List<MissionSet> missionSets) {
        return missionSets.stream()
                .collect(Collectors.groupingBy(
                        MissionSet::getStarLevel,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private boolean isStarLevelUnlocked(
            int starLevel,
            Map<Integer, List<MissionSet>> setsByStarLevel,
            Map<String, MissionSetProgress> progressBySetId
    ) {
        if (starLevel == 1) {
            return true;
        }
        return isStarLevelCleared(starLevel - 1, setsByStarLevel, progressBySetId);
    }

    private boolean isStarLevelCleared(
            int starLevel,
            Map<Integer, List<MissionSet>> setsByStarLevel,
            Map<String, MissionSetProgress> progressBySetId
    ) {
        return setsByStarLevel.getOrDefault(starLevel, List.of())
                .stream()
                .anyMatch(set -> progressBySetId.containsKey(set.getSetId()));
    }

    private Map<String, MissionSetProgress> progressBySetId(UUID childId, Collection<MissionSet> missionSets) {
        List<String> setIds = missionSets.stream().map(MissionSet::getSetId).toList();
        if (setIds.isEmpty()) {
            return Map.of();
        }
        return missionSetProgressRepository.findAllByChildIdAndSetIdIn(childId, setIds)
                .stream()
                .collect(Collectors.toMap(
                        MissionSetProgress::getSetId,
                        progress -> progress
                ));
    }

    private MissionListResponse getLegacyMissions(UUID childId) {
        StageProgressResponse stageProgress = new StageProgressResponse(
                legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 1),
                legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 2),
                legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)
        );
        List<MissionListResponse.StageResponse> stages = legacyMissionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        Mission::getStage,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> new MissionListResponse.StageResponse(
                        entry.getKey(),
                        "Stage " + entry.getKey(),
                        entry.getValue().stream()
                                .map(mission -> {
                                    boolean completed = legacyMissionAttemptRepository
                                            .findLatestCompletedAt(childId, mission.getId())
                                            .isPresent();
                                    boolean unlocked = isUnlocked(mission, stageProgress);
                                    return new MissionListResponse.MissionResponse(
                                            mission.getId(),
                                            mission.getMissionCode(),
                                            mission.getTitle(),
                                            mission.getDescription(),
                                            unlocked,
                                            List.of(new MissionListResponse.StarLevelResponse(
                                                    1,
                                                    MissionListResponse.labelForStar(1),
                                                    1,
                                                    completed ? 1 : 0,
                                                    unlocked,
                                                    completed
                                            )),
                                            mission.getStage()
                                    );
                                })
                                .toList()
                ))
                .toList();
        long completedSetCount = stageProgress.stage1Completed()
                + stageProgress.stage2Completed()
                + stageProgress.stage3Completed();
        long totalSetCount = stages.stream()
                .flatMap(stage -> stage.missions().stream())
                .count();
        return new MissionListResponse(
                stages,
                new MissionListResponse.ProgressResponse(completedSetCount, totalSetCount)
        );
    }
}
