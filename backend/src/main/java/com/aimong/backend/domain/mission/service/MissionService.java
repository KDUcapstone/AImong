package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionSummaryResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final ChildActivityService childActivityService;

    @Transactional(readOnly = true)
    public MissionListResponse getMissions(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        StageProgressResponse stageProgress = new StageProgressResponse(
                countCompletedMissionByStage(childId, (short) 1),
                countCompletedMissionByStage(childId, (short) 2),
                countCompletedMissionByStage(childId, (short) 3)
        );

        List<MissionSummaryResponse> missions = missionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc()
                .stream()
                .map(mission -> toMissionSummary(childId, mission, stageProgress))
                .toList();

        return new MissionListResponse(missions, stageProgress);
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
        return countCompletedMissionByStage(childId, (short) (mission.getStage() - 1)) >= 4;
    }

    private MissionSummaryResponse toMissionSummary(UUID childId, Mission mission, StageProgressResponse stageProgress) {
        LocalDate completedAt = missionAttemptRepository.findLatestCompletedAt(
                        childId,
                        mission.getId()
                )
                .orElse(null);
        boolean isCompleted = completedAt != null;

        return new MissionSummaryResponse(
                mission.getId(),
                mission.getStage(),
                mission.getTitle(),
                mission.getDescription(),
                isUnlockedForChild(childId, mission, stageProgress),
                isCompleted,
                completedAt,
                isCompleted
        );
    }

    private long countCompletedMissionByStage(UUID childId, short stage) {
        return missionAttemptRepository.countCompletedMissionByStage(
                childId,
                stage
        );
    }
}
