package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionSummaryResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionAttempt;
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

    @Transactional(readOnly = true)
    public MissionListResponse getMissions(UUID childId) {
        StageProgressResponse stageProgress = new StageProgressResponse(
                missionAttemptRepository.countCompletedMissionByStage(childId, (short) 1),
                missionAttemptRepository.countCompletedMissionByStage(childId, (short) 2),
                missionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)
        );

        List<MissionSummaryResponse> missions = missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc()
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

    private MissionSummaryResponse toMissionSummary(UUID childId, Mission mission, StageProgressResponse stageProgress) {
        MissionAttempt latestAttempt = missionAttemptRepository
                .findTopByChildIdAndMissionIdOrderBySubmittedAtDesc(childId, mission.getId())
                .orElse(null);

        LocalDate completedAt = latestAttempt != null ? latestAttempt.getAttemptDate() : null;

        return new MissionSummaryResponse(
                mission.getId(),
                mission.getStage(),
                mission.getTitle(),
                mission.getDescription(),
                isUnlocked(mission, stageProgress),
                latestAttempt != null,
                completedAt,
                latestAttempt != null
        );
    }
}
