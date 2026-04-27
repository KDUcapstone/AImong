package com.aimong.backend.domain.mission.service.generation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class KerisCurriculumRegistry {

    private static final Path KERIS_DOC_ROOT = Path.of("docs", "keris");
    private static final Path STAGE_MAP_PATH = KERIS_DOC_ROOT.resolve("01_stage_map.yaml");
    private static final Path MISSION_RULES_PATH = KERIS_DOC_ROOT.resolve("02_mission_rules.yaml");

    private final List<KerisMissionRule> missionRules;
    private final KerisStageMapSummary stageMapSummary;

    public KerisCurriculumRegistry() {
        this.stageMapSummary = loadStageMapSummary();
        this.missionRules = loadMissionRules();
    }

    public KerisStageMapSummary stageMapSummary() {
        return stageMapSummary;
    }

    public List<KerisMissionRule> missionRules() {
        return List.copyOf(missionRules);
    }

    public Optional<KerisMissionRule> findMissionRule(String missionCode) {
        return missionRules.stream()
                .filter(rule -> rule.missionCode().equals(missionCode))
                .findFirst();
    }

    public Optional<KerisMissionRule> findMissionRuleByStageAndTitle(int stage, String title) {
        return missionRules.stream()
                .filter(rule -> rule.stage() == stage)
                .filter(rule -> rule.missionTitle().equals(title))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private KerisStageMapSummary loadStageMapSummary() {
        Map<String, Object> root = readYamlMap(STAGE_MAP_PATH);
        Map<String, Object> summary = (Map<String, Object>) root.getOrDefault("summary", Map.of());
        return new KerisStageMapSummary(
                ((Number) summary.getOrDefault("totalStages", 0)).intValue(),
                ((Number) summary.getOrDefault("totalMissions", 0)).intValue(),
                ((Number) summary.getOrDefault("expandedSeedQuestionCount", 0)).intValue(),
                ((Number) summary.getOrDefault("expandedQuestionsPerMission", 0)).intValue(),
                ((Number) summary.getOrDefault("packsPerMission", 0)).intValue(),
                ((Number) summary.getOrDefault("questionsPerPack", 0)).intValue()
        );
    }

    @SuppressWarnings("unchecked")
    private List<KerisMissionRule> loadMissionRules() {
        Map<String, Object> root = readYamlMap(MISSION_RULES_PATH);
        List<Map<String, Object>> missions = (List<Map<String, Object>>) root.getOrDefault("missions", List.of());
        List<KerisMissionRule> results = new ArrayList<>();
        for (Map<String, Object> mission : missions) {
            results.add(new KerisMissionRule(
                    Objects.toString(mission.get("missionCode"), ""),
                    ((Number) mission.getOrDefault("stage", 0)).intValue(),
                    Objects.toString(mission.get("stageTitle"), ""),
                    Objects.toString(mission.get("missionTitle"), ""),
                    toStringList(mission.get("allowedConcepts")),
                    toStringList(mission.get("bannedConcepts")),
                    toStringList(mission.get("preferredQuestionTypes")),
                    toStringList(mission.get("preferredContentTags")),
                    Objects.toString(mission.get("curriculumRef"), ""),
                    toStringList(mission.get("kerisRefs")),
                    toStringList(mission.get("goodScenarioPatterns")),
                    toStringList(mission.get("avoidScenarioPatterns"))
            ));
        }
        return results;
    }

    private Map<String, Object> readYamlMap(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("KERIS curriculum asset not found: " + path);
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object loaded = yaml.load(inputStream);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
                return normalized;
            }
            throw new IllegalStateException("KERIS curriculum asset is not a map: " + path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read KERIS curriculum asset: " + path, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return ((List<Object>) list).stream()
                .map(String::valueOf)
                .toList();
    }

    public record KerisStageMapSummary(
            int totalStages,
            int totalMissions,
            int expandedSeedQuestionCount,
            int expandedQuestionsPerMission,
            int packsPerMission,
            int questionsPerPack
    ) {
    }

    public record KerisMissionRule(
            String missionCode,
            int stage,
            String stageTitle,
            String missionTitle,
            List<String> allowedConcepts,
            List<String> bannedConcepts,
            List<String> preferredQuestionTypes,
            List<String> preferredContentTags,
            String curriculumRef,
            List<String> kerisRefs,
            List<String> goodScenarioPatterns,
            List<String> avoidScenarioPatterns
    ) {
    }
}
