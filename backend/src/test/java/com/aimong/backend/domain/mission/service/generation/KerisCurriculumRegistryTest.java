package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KerisCurriculumRegistryTest {

    @Test
    void registryLoadsStructuredKerisAssetsWithoutRawPdfDependency() {
        KerisCurriculumRegistry registry = new KerisCurriculumRegistry();

        assertThat(registry.stageMapSummary().totalStages()).isEqualTo(3);
        assertThat(registry.stageMapSummary().totalMissions()).isEqualTo(16);
        assertThat(registry.stageMapSummary().expandedSeedQuestionCount()).isEqualTo(960);
        assertThat(registry.stageMapSummary().packsPerMission()).isEqualTo(6);
        assertThat(registry.stageMapSummary().questionsPerPack()).isEqualTo(10);
        assertThat(registry.missionRules()).hasSize(16);
        assertThat(registry.findMissionRule("S0203")).isPresent();
        assertThat(registry.findMissionRule("S0203").orElseThrow().preferredContentTags())
                .contains("PRIVACY");
    }
}
