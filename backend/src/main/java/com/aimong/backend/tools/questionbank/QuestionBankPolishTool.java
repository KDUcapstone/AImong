package com.aimong.backend.tools.questionbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestionBankPolishTool {

    private QuestionBankPolishTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "usage: QuestionBankPolishTool <input-json> <output-json> <report-md> <report-json>"
            );
        }

        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path reportMd = Path.of(args[2]);
        Path reportJson = Path.of(args[3]);

        JsonNode root = objectMapper.readTree(Files.readString(input));
        QuestionBankAuditLoader loader = new QuestionBankAuditLoader(objectMapper);
        AuditQuestionBank beforeBank = loader.load(input);

        PolishingTargetExtractor extractor = new PolishingTargetExtractor();
        PolishingTargetExtractor.TargetReport beforeTargets = extractor.extract(beforeBank);
        Set<String> explanationTargetIds = beforeTargets.explanationTargetIds();
        Set<String> optionTargetIds = beforeTargets.optionTargetIds();

        ExplanationPolishingRewriter explanationRewriter = new ExplanationPolishingRewriter();
        OptionStylePolishingRewriter optionRewriter = new OptionStylePolishingRewriter();

        ArrayNode questions = (ArrayNode) root.get("questions");
        Map<String, List<String>> changeReasons = new LinkedHashMap<>();

        for (JsonNode node : questions) {
            ObjectNode questionNode = (ObjectNode) node;
            String externalId = questionNode.path("externalId").asText();
            AuditQuestion question = toAuditQuestion(questionNode, objectMapper);
            if (explanationTargetIds.contains(externalId)) {
                questionNode.put("explanation", explanationRewriter.rewrite(question));
                changeReasons.computeIfAbsent(externalId, ignored -> new java.util.ArrayList<>())
                        .add("explanation_suffix_polish");
            }
            if (optionTargetIds.contains(externalId)) {
                ArrayNode updatedOptions = objectMapper.createArrayNode();
                optionRewriter.rewrite(question).forEach(updatedOptions::add);
                questionNode.set("options", updatedOptions);
                changeReasons.computeIfAbsent(externalId, ignored -> new java.util.ArrayList<>())
                        .add("option_style_polish");
            }
        }

        if (root instanceof ObjectNode objectNode) {
            objectNode.put("generationVersion", root.path("generationVersion").asText() + "-polished");
            objectNode.put(
                    "normalizationNote",
                    root.path("normalizationNote").asText() + " / v4 warning polish: explanation suffix and option style only."
            );
        }

        writeParent(output);
        objectMapper.writeValue(output.toFile(), root);

        AuditQuestionBank afterBank = loader.load(output);
        PolishingTargetExtractor.TargetReport afterTargets = extractor.extract(afterBank);
        PostPolishAuditRunner.PolishReport polishReport =
                new PostPolishAuditRunner().compare(beforeBank, afterBank, beforeTargets, afterTargets);

        writeParent(reportJson);
        objectMapper.writeValue(reportJson.toFile(), new QuestionBankPolishReport(
                polishReport,
                changeReasons,
                beforeTargets,
                afterTargets
        ));
        Files.writeString(reportMd, renderMarkdown(polishReport, changeReasons, beforeTargets, afterTargets));
    }

    private static AuditQuestion toAuditQuestion(ObjectNode node, ObjectMapper objectMapper) throws Exception {
        return new AuditQuestion(
                node.path("externalId").asText(""),
                node.path("missionCode").asText(""),
                (short) node.path("stage").asInt(0),
                node.path("stageTitle").asText(""),
                node.path("missionTitle").asText(""),
                com.aimong.backend.domain.mission.entity.QuestionType.valueOf(node.path("type").asText()),
                node.path("question").asText(""),
                toStringList(node.get("options")),
                objectMapper.treeToValue(node.get("answer"), Object.class),
                node.path("explanation").asText(""),
                toStringList(node.get("contentTags")),
                node.path("curriculumRef").asText(""),
                node.path("difficulty").asInt(0),
                com.aimong.backend.domain.mission.entity.DifficultyBand.valueOf(node.path("difficultyBand").asText()),
                node.path("packNo").asInt(0),
                node.path("sourceType").asText("")
        );
    }

    private static List<String> toStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<String> values = new java.util.ArrayList<>();
        node.forEach(item -> values.add(item.asText("")));
        return List.copyOf(values);
    }

    private static String renderMarkdown(
            PostPolishAuditRunner.PolishReport report,
            Map<String, List<String>> changeReasons,
            PolishingTargetExtractor.TargetReport beforeTargets,
            PolishingTargetExtractor.TargetReport afterTargets
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Question Bank v4 Polish Report\n\n");
        sb.append("- Verdict: ").append(report.verdict()).append('\n');
        report.changedMetrics().forEach(line -> sb.append("- ").append(line).append('\n'));
        sb.append("- changedQuestionCount: ").append(changeReasons.size()).append('\n');
        sb.append("- explanationClusterCount: ").append(report.explanationClusterCountBefore())
                .append(" -> ").append(report.explanationClusterCountAfter()).append('\n');
        sb.append("- optionStyleWarningCount: ").append(report.optionStyleWarningCountBefore())
                .append(" -> ").append(report.optionStyleWarningCountAfter()).append('\n');
        sb.append("\n## Explanation Targets\n\n");
        beforeTargets.explanationClusters().stream().limit(10).forEach(cluster -> {
            sb.append("- ").append(cluster.suffix()).append(": ").append(cluster.count()).append('\n');
        });
        sb.append("\n## Remaining Explanation Targets\n\n");
        afterTargets.explanationClusters().stream().limit(10).forEach(cluster -> {
            sb.append("- ").append(cluster.suffix()).append(": ").append(cluster.count()).append('\n');
        });
        sb.append("\n## Changed Questions\n\n");
        changeReasons.entrySet().stream().limit(120).forEach(entry -> {
            sb.append("- ").append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue())).append('\n');
        });
        return sb.toString();
    }

    private static void writeParent(Path path) throws Exception {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    public record QuestionBankPolishReport(
            PostPolishAuditRunner.PolishReport summary,
            Map<String, List<String>> changedQuestions,
            PolishingTargetExtractor.TargetReport beforeTargets,
            PolishingTargetExtractor.TargetReport afterTargets
    ) {
    }
}
