package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InitialSeedExecutionGuard {

    private final MissionRepository missionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionGenerationProperties generationProperties;

    public SeedExecutionStatus inspect() {
        List<Mission> activeMissions = missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc();
        long totalMissionCount = activeMissions.size();
        long totalQuestionCount = questionBankRepository.countByIsActiveTrue();
        boolean allMissionsReady = activeMissions.stream()
                .allMatch(mission -> questionBankRepository.countByMissionIdAndIsActiveTrue(mission.getId())
                        >= generationProperties.targetPoolPerMission());

        return new SeedExecutionStatus(
                totalMissionCount,
                totalQuestionCount,
                allMissionsReady,
                totalQuestionCount >= totalMissionCount * generationProperties.targetPoolPerMission()
        );
    }

    public record SeedExecutionStatus(
            long activeMissionCount,
            long activeQuestionCount,
            boolean allMissionsAtTargetPool,
            boolean globalTargetReached
    ) {
        public boolean shouldSkipPersistedSeed() {
            return activeMissionCount > 0 && allMissionsAtTargetPool && globalTargetReached;
        }
    }
}
