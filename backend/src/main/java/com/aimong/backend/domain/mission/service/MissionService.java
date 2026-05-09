package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionSummaryResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MissionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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
        List<MissionSet> missionSets = missionSetRepository.findAllByActiveTrueOrderByLevelNoAscStageAscDisplayOrderAscSetIdAsc();
        Map<String, com.aimong.backend.domain.mission.entity.MissionSetProgress> progressBySetId =
                missionSetProgressRepository.findAllByChildIdAndSetIdIn(childId, setIds(missionSets))
                        .stream()
                        .collect(Collectors.toMap(
                                com.aimong.backend.domain.mission.entity.MissionSetProgress::getSetId,
                                progress -> progress
                        ));

        List<MissionListResponse.LevelResponse> levels = missionSets.stream()
                .collect(Collectors.groupingBy(MissionSet::getLevelNo))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toLevelResponse(entry.getKey(), childId, entry.getValue(), progressBySetId))
                .toList();

        long completedSetCount = progressBySetId.size();
        long totalSetCount = missionSets.size();
        int currentLevelNo = levels.stream()
                .filter(level -> level.completedSetCount() < level.totalSetCount())
                .map(MissionListResponse.LevelResponse::levelNo)
                .findFirst()
                .orElse(levels.isEmpty() ? 1 : levels.get(levels.size() - 1).levelNo());

        return new MissionListResponse(
                levels,
                new MissionListResponse.ProgressResponse(completedSetCount, totalSetCount, currentLevelNo)
        );
    }

    public boolean isUnlocked(Mission mission, StageProgressResponse stageProgress) {
        return switch (mission.getStage()) {
            case 1 -> true;
            case 2 -> stageProgress.stage1Completed() >= 3;
            case 3 -> stageProgress.stage2Completed() >= 4;
            default -> false;
        };
    }

    public boolean isUnlockedForChild(UUID childId, Mission mission, StageProgressResponse stageProgress) {
        if (mission.getStage() <= 3) {
            return isUnlocked(mission, stageProgress);
        }
        return true;
    }

    public StageProgressResponse stageProgressForLegacy(UUID childId) {
        List<MissionSet> missionSets = missionSetRepository.findAllByActiveTrueOrderByLevelNoAscStageAscDisplayOrderAscSetIdAsc();
        Map<Short, List<String>> setIdsByStage = missionSets.stream()
                .filter(set -> set.getLevelNo() == 1)
                .collect(Collectors.groupingBy(MissionSet::getStage, Collectors.mapping(MissionSet::getSetId, Collectors.toList())));
        return new StageProgressResponse(
                countCompletedSets(childId, setIdsByStage.getOrDefault((short) 1, List.of())),
                countCompletedSets(childId, setIdsByStage.getOrDefault((short) 2, List.of())),
                countCompletedSets(childId, setIdsByStage.getOrDefault((short) 3, List.of()))
        );
    }

    public boolean isUnlocked(UUID childId, MissionSet missionSet) {
        if (missionSet.getLevelNo() > 1) {
            List<String> previousLevelSetIds = missionSetRepository.findAllByActiveTrueOrderByLevelNoAscStageAscDisplayOrderAscSetIdAsc()
                    .stream()
                    .filter(set -> set.getLevelNo() == missionSet.getLevelNo() - 1)
                    .map(MissionSet::getSetId)
                    .toList();
            return !previousLevelSetIds.isEmpty()
                    && countCompletedSets(childId, previousLevelSetIds) >= previousLevelSetIds.size();
        }

        long stage1Completed = countCompletedSets(childId, setIdsForLevelAndStage(1, (short) 1));
        long stage2Completed = countCompletedSets(childId, setIdsForLevelAndStage(1, (short) 2));
        return switch (missionSet.getStage()) {
            case 1 -> true;
            case 2 -> stage1Completed >= 3;
            case 3 -> stage2Completed >= 4;
            default -> false;
        };
    }

    public MissionSet resolvePlayableSet(UUID childId, UUID missionId) {
        return missionSetRepository.findAllByMissionIdAndActiveTrueOrderByLevelNoAscDisplayOrderAscSetIdAsc(missionId)
                .stream()
                .filter(set -> isUnlocked(childId, set))
                .filter(set -> !missionSetProgressRepository.existsByChildIdAndSetId(childId, set.getSetId()))
                .findFirst()
                .or(() -> missionSetRepository.findAllByMissionIdAndActiveTrueOrderByLevelNoAscDisplayOrderAscSetIdAsc(missionId)
                        .stream()
                        .filter(set -> isUnlocked(childId, set))
                        .findFirst())
                .orElseThrow(() -> new com.aimong.backend.global.exception.AimongException(
                        com.aimong.backend.global.exception.ErrorCode.MISSION_SET_LOCKED
                ));
    }

    private MissionListResponse.LevelResponse toLevelResponse(
            int levelNo,
            UUID childId,
            List<MissionSet> missionSets,
            Map<String, com.aimong.backend.domain.mission.entity.MissionSetProgress> progressBySetId
    ) {
        List<MissionListResponse.StageResponse> stages = missionSets.stream()
                .collect(Collectors.groupingBy(MissionSet::getStage))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toStageResponse(entry.getKey(), childId, entry.getValue(), progressBySetId))
                .toList();
        long completed = stages.stream().mapToLong(MissionListResponse.StageResponse::completedSetCount).sum();
        long total = stages.stream().mapToLong(MissionListResponse.StageResponse::totalSetCount).sum();
        String difficulty = missionSets.stream()
                .min(Comparator.comparing(MissionSet::getSetId))
                .map(set -> set.getDifficulty().name())
                .orElse("LOW");
        boolean unlocked = levelNo == 1 || completed > 0 || stages.stream()
                .flatMap(stage -> stage.sets().stream())
                .anyMatch(MissionListResponse.SetResponse::isUnlocked);
        return new MissionListResponse.LevelResponse(levelNo, difficulty, unlocked, completed, total, stages);
    }

    private MissionListResponse.StageResponse toStageResponse(
            short stage,
            UUID childId,
            List<MissionSet> missionSets,
            Map<String, com.aimong.backend.domain.mission.entity.MissionSetProgress> progressBySetId
    ) {
        List<MissionListResponse.SetResponse> sets = missionSets.stream()
                .sorted(Comparator.comparing(MissionSet::getDisplayOrder).thenComparing(MissionSet::getSetId))
                .map(set -> toSetResponse(childId, set, progressBySetId))
                .toList();
        long completed = sets.stream().filter(MissionListResponse.SetResponse::isCompleted).count();
        return new MissionListResponse.StageResponse(stage, completed, sets.size(), sets);
    }

    private MissionListResponse.SetResponse toSetResponse(
            UUID childId,
            MissionSet missionSet,
            Map<String, com.aimong.backend.domain.mission.entity.MissionSetProgress> progressBySetId
    ) {
        com.aimong.backend.domain.mission.entity.MissionSetProgress progress = progressBySetId.get(missionSet.getSetId());
        boolean completed = progress != null;
        return new MissionListResponse.SetResponse(
                missionSet.getSetId(),
                missionSet.getMissionId(),
                missionSet.getMissionCode(),
                missionSet.getLevelNo(),
                missionSet.getStage(),
                missionSet.getDifficulty().name(),
                missionSet.getTitle(),
                missionSet.getDescription(),
                isUnlocked(childId, missionSet),
                completed,
                completed ? progress.getCompletedAt().atZone(KST).toLocalDate() : null,
                completed
        );
    }

    private List<String> setIds(Collection<MissionSet> missionSets) {
        return missionSets.stream().map(MissionSet::getSetId).toList();
    }

    private List<String> setIdsForLevelAndStage(int levelNo, short stage) {
        return missionSetRepository.findAllByActiveTrueOrderByLevelNoAscStageAscDisplayOrderAscSetIdAsc()
                .stream()
                .filter(set -> set.getLevelNo() == levelNo && set.getStage() == stage)
                .map(MissionSet::getSetId)
                .toList();
    }

    private long countCompletedSets(UUID childId, List<String> setIds) {
        if (setIds.isEmpty()) {
            return 0;
        }
        return missionSetProgressRepository.countByChildIdAndSetIdIn(childId, setIds);
    }

    private MissionListResponse getLegacyMissions(UUID childId) {
        StageProgressResponse stageProgress = new StageProgressResponse(
                legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 1),
                legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 2),
                legacyMissionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)
        );
        List<MissionSummaryResponse> missions = legacyMissionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc()
                .stream()
                .map(mission -> {
                    LocalDate completedAt = legacyMissionAttemptRepository.findLatestCompletedAt(childId, mission.getId()).orElse(null);
                    boolean completed = completedAt != null;
                    return new MissionSummaryResponse(
                            mission.getId(),
                            mission.getStage(),
                            mission.getTitle(),
                            mission.getDescription(),
                            isUnlocked(mission, stageProgress),
                            completed,
                            completedAt,
                            completed
                    );
                })
                .toList();
        return new MissionListResponse(missions, stageProgress);
    }
}
