package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MissionCodeResolver {

    private final MissionRepository missionRepository;
    private final KerisCurriculumRegistry kerisCurriculumRegistry;

    public Optional<String> resolve(Mission mission) {
        if (mission.getMissionCode() != null && !mission.getMissionCode().isBlank()) {
            return Optional.of(mission.getMissionCode());
        }

        Optional<String> byTitle = kerisCurriculumRegistry.findMissionRuleByStageAndTitle(mission.getStage(), mission.getTitle())
                .map(KerisCurriculumRegistry.KerisMissionRule::missionCode);
        if (byTitle.isPresent()) {
            return byTitle;
        }

        List<Mission> stageMissions = missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc().stream()
                .filter(activeMission -> activeMission.getStage() == mission.getStage())
                .toList();
        int index = indexOf(stageMissions, mission.getId());
        if (index < 0) {
            return Optional.empty();
        }

        List<KerisCurriculumRegistry.KerisMissionRule> stageRules = kerisCurriculumRegistry.missionRules().stream()
                .filter(rule -> rule.stage() == mission.getStage())
                .sorted(Comparator.comparing(KerisCurriculumRegistry.KerisMissionRule::missionCode))
                .toList();

        if (index >= stageRules.size()) {
            return Optional.empty();
        }

        return Optional.of(stageRules.get(index).missionCode());
    }

    public Map<UUID, String> resolveAllActiveMissionCodes() {
        return missionRepository.findAllByIsActiveTrueOrderByStageAscIdAsc().stream()
                .map(mission -> Map.entry(mission.getId(), resolve(mission).orElse(null)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private int indexOf(List<Mission> missions, UUID missionId) {
        for (int index = 0; index < missions.size(); index++) {
            if (missions.get(index).getId().equals(missionId)) {
                return index;
            }
        }
        return -1;
    }
}
