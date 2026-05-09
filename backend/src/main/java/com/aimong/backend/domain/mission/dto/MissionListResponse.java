package com.aimong.backend.domain.mission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

public record MissionListResponse(
        List<LevelResponse> levels,
        ProgressResponse progress
) {
    public MissionListResponse(List<MissionSummaryResponse> missions, StageProgressResponse stageProgress) {
        this(
                List.of(
                        new LevelResponse(1, "LOW", true, stageProgress.stage1Completed(), 0, List.of(
                                new StageResponse(1, stageProgress.stage1Completed(), 0, legacySetsForStage(missions, (short) 1))
                        )),
                        new LevelResponse(2, "LOW", true, stageProgress.stage2Completed(), 0, List.of(
                                new StageResponse(2, stageProgress.stage2Completed(), 0, legacySetsForStage(missions, (short) 2))
                        )),
                        new LevelResponse(3, "LOW", true, stageProgress.stage3Completed(), 0, List.of(
                                new StageResponse(3, stageProgress.stage3Completed(), 0, legacySetsForStage(missions, (short) 3))
                        ))
                ),
                new ProgressResponse(
                        stageProgress.stage1Completed() + stageProgress.stage2Completed() + stageProgress.stage3Completed(),
                        missions.size(),
                        1
                )
        );
    }

    @JsonProperty("missions")
    public List<MissionSummaryResponse> missions() {
        return levels.stream()
                .flatMap(level -> level.stages().stream())
                .flatMap(stage -> stage.sets().stream())
                .map(set -> new MissionSummaryResponse(
                        set.missionId(),
                        (short) set.stage(),
                        set.title(),
                        set.description(),
                        set.isUnlocked(),
                        set.isCompleted(),
                        set.completedAt(),
                        set.isReviewable()
                ))
                .toList();
    }

    @JsonProperty("stageProgress")
    public StageProgressResponse stageProgress() {
        return new StageProgressResponse(
                completedForStage(1),
                completedForStage(2),
                completedForStage(3)
        );
    }

    private long completedForStage(int stageNo) {
        return levels.stream()
                .flatMap(level -> level.stages().stream())
                .filter(stage -> stage.stage() == stageNo)
                .mapToLong(StageResponse::completedSetCount)
                .sum();
    }

    private static List<SetResponse> legacySetsForStage(List<MissionSummaryResponse> missions, short stage) {
        return missions.stream()
                .filter(mission -> mission.stage() == stage)
                .map(mission -> new SetResponse(
                        null,
                        mission.id(),
                        null,
                        1,
                        mission.stage(),
                        "LOW",
                        mission.title(),
                        mission.description(),
                        mission.isUnlocked(),
                        mission.isCompleted(),
                        mission.completedAt(),
                        mission.isReviewable()
                ))
                .toList();
    }

    public record LevelResponse(
            int levelNo,
            String difficulty,
            boolean isUnlocked,
            long completedSetCount,
            long totalSetCount,
            List<StageResponse> stages
    ) {
    }

    public record StageResponse(
            int stage,
            long completedSetCount,
            long totalSetCount,
            List<SetResponse> sets
    ) {
    }

    public record SetResponse(
            String setId,
            UUID missionId,
            String missionCode,
            int levelNo,
            int stage,
            String difficulty,
            String title,
            String description,
            boolean isUnlocked,
            boolean isCompleted,
            LocalDate completedAt,
            boolean isReviewable
    ) {
    }

    public record ProgressResponse(
            long completedSetCount,
            long totalSetCount,
            int currentLevelNo
    ) {
    }
}
