package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import org.junit.jupiter.api.Test;

class PackQuotaPlannerTest {

    private final PackQuotaPlanner planner = new PackQuotaPlanner(
            new QuestionGenerationProperties(60, 6, 10, 36, 18, 10, 30000L, 10, 2)
    );

    @Test
    void packQuotaMatchesExactTemplate() {
        PackQuotaPlanner.PackQuota pack1 = planner.planForPack(1);
        PackQuotaPlanner.PackQuota pack5 = planner.planForPack(5);

        assertThat(pack1.questionCount()).isEqualTo(10);
        assertThat(pack1.typeQuota()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                QuestionType.OX, 2,
                QuestionType.MULTIPLE, 3,
                QuestionType.FILL, 2,
                QuestionType.SITUATION, 3
        ));
        assertThat(pack1.difficultyQuota()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                DifficultyBand.LOW, 5,
                DifficultyBand.MEDIUM, 3,
                DifficultyBand.HIGH, 2
        ));
        assertThat(pack5.difficultyQuota()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                DifficultyBand.LOW, 5,
                DifficultyBand.MEDIUM, 4,
                DifficultyBand.HIGH, 1
        ));
    }

    @Test
    void missionQuotaMatchesExactSixtyQuestionPolicy() {
        PackQuotaPlanner.MissionQuota missionQuota = planner.planForMission();

        assertThat(missionQuota.packCount()).isEqualTo(6);
        assertThat(missionQuota.questionCount()).isEqualTo(60);
        assertThat(missionQuota.typeQuota()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                QuestionType.OX, 12,
                QuestionType.MULTIPLE, 18,
                QuestionType.FILL, 12,
                QuestionType.SITUATION, 18
        ));
        assertThat(missionQuota.difficultyQuota()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                DifficultyBand.LOW, 30,
                DifficultyBand.MEDIUM, 20,
                DifficultyBand.HIGH, 10
        ));
        assertThat(missionQuota.packs()).hasSize(6);
        assertThat(missionQuota.packs().stream().mapToInt(PackQuotaPlanner.PackQuota::questionCount).sum()).isEqualTo(60);
        assertThat(missionQuota.packs().stream().mapToInt(pack -> pack.difficultyQuota().get(DifficultyBand.LOW)).sum()).isEqualTo(30);
        assertThat(missionQuota.packs().stream().mapToInt(pack -> pack.difficultyQuota().get(DifficultyBand.MEDIUM)).sum()).isEqualTo(20);
        assertThat(missionQuota.packs().stream().mapToInt(pack -> pack.difficultyQuota().get(DifficultyBand.HIGH)).sum()).isEqualTo(10);
    }
}
