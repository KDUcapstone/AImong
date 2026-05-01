package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.service.generation.AnswerQualityValidator;
import com.aimong.backend.domain.mission.service.generation.CurriculumFitValidator;
import com.aimong.backend.domain.mission.service.generation.ElementaryReadabilityValidator;
import com.aimong.backend.domain.mission.service.generation.ExplanationQualityValidator;
import com.aimong.backend.domain.mission.service.generation.KerisCurriculumRegistry;
import com.aimong.backend.domain.mission.service.generation.KerisGoldExampleRegistry;
import com.aimong.backend.domain.mission.service.generation.KoreanSurfaceLintValidator;
import com.aimong.backend.domain.mission.service.generation.NaturalnessValidator;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationReport;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationService;
import com.aimong.backend.domain.mission.service.generation.SchemaValidator;
import com.aimong.backend.domain.mission.service.generation.SafetyValidator;
import com.aimong.backend.domain.mission.service.generation.SimilarityDeduplicator;
import com.aimong.backend.domain.mission.service.generation.Step3VocabularyCeilingValidator;
import com.aimong.backend.domain.mission.service.generation.StructureRuleValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CurrentCriteriaQuestionBankAuditTest {

    private static final Path SOURCE =
            Path.of("_generated/question-bank/question-bank-960-diversified-v4-polished.json");
    private static final Path REPORT_JSON =
            Path.of("_generated/reports/question-bank-current-criteria-full-validation.json");
    private static final Path REPORT_MD =
            Path.of("_generated/reports/question-bank-current-criteria-full-validation.md");
    private static final Path FIXED_SOURCE =
            Path.of("_generated/question-bank/question-bank-960-diversified-v4-polished-current-criteria-fixed.json");
    private static final Path FIXED_REPORT_JSON =
            Path.of("_generated/reports/question-bank-current-criteria-full-validation-fixed.json");
    private static final Path FIXED_REPORT_MD =
            Path.of("_generated/reports/question-bank-current-criteria-full-validation-fixed.md");

    @Test
    void auditPolished960BankAgainstCurrentValidators() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        QuestionBankAuditLoader loader = new QuestionBankAuditLoader(objectMapper);
        AuditQuestionBank bank = loader.load(SOURCE);

        QuestionValidationService validationService = new QuestionValidationService(
                new SchemaValidator(),
                new SafetyValidator(),
                new CurriculumFitValidator(new KerisCurriculumRegistry()),
                new StructureRuleValidator(new KerisCurriculumRegistry()),
                new ElementaryReadabilityValidator(),
                new AnswerQualityValidator(),
                new ExplanationQualityValidator(),
                new NaturalnessValidator(),
                new KoreanSurfaceLintValidator(),
                new Step3VocabularyCeilingValidator(),
                new SimilarityDeduplicator(),
                new KerisGoldExampleRegistry(objectMapper),
                objectMapper
        );

        List<QuestionResult> results = bank.questions().stream()
                .map(question -> validate(validationService, question))
                .toList();

        long passedCount = results.stream().filter(QuestionResult::pass).count();
        List<QuestionResult> failed = results.stream().filter(result -> !result.pass()).toList();

        Map<String, Long> hardFailCounts = failed.stream()
                .flatMap(result -> result.hardFailReasons().stream())
                .collect(Collectors.groupingBy(reason -> reason, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> warningCounts = results.stream()
                .flatMap(result -> result.softWarnings().stream())
                .collect(Collectors.groupingBy(reason -> reason, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> decisionCounts = results.stream()
                .collect(Collectors.groupingBy(QuestionResult::recommendedAction, LinkedHashMap::new, Collectors.counting()));

        FullAuditReport report = new FullAuditReport(
                SOURCE.toString(),
                bank.totalQuestionCount(),
                passedCount,
                failed.size(),
                decisionCounts,
                hardFailCounts,
                warningCounts,
                failed.stream().limit(200).toList()
        );

        Files.createDirectories(REPORT_JSON.getParent());
        objectMapper.writeValue(REPORT_JSON.toFile(), report);
        Files.writeString(REPORT_MD, renderMarkdown(report));

        if (failed.size() == 1 && "S0304-P4-06".equals(failed.getFirst().externalId())) {
            writeFixedQuestionBank(objectMapper);
            AuditQuestionBank fixedBank = loader.load(FIXED_SOURCE);
            List<QuestionResult> fixedResults = fixedBank.questions().stream()
                    .map(question -> validate(validationService, question))
                    .toList();
            long fixedPassedCount = fixedResults.stream().filter(QuestionResult::pass).count();
            List<QuestionResult> fixedFailed = fixedResults.stream().filter(result -> !result.pass()).toList();
            FullAuditReport fixedReport = new FullAuditReport(
                    FIXED_SOURCE.toString(),
                    fixedBank.totalQuestionCount(),
                    fixedPassedCount,
                    fixedFailed.size(),
                    fixedResults.stream().collect(Collectors.groupingBy(
                            QuestionResult::recommendedAction,
                            LinkedHashMap::new,
                            Collectors.counting()
                    )),
                    fixedFailed.stream()
                            .flatMap(result -> result.hardFailReasons().stream())
                            .collect(Collectors.groupingBy(reason -> reason, LinkedHashMap::new, Collectors.counting())),
                    fixedResults.stream()
                            .flatMap(result -> result.softWarnings().stream())
                            .collect(Collectors.groupingBy(reason -> reason, LinkedHashMap::new, Collectors.counting())),
                    fixedFailed
            );
            objectMapper.writeValue(FIXED_REPORT_JSON.toFile(), fixedReport);
            Files.writeString(FIXED_REPORT_MD, renderMarkdown(fixedReport));
            assertThat(fixedFailed).isEmpty();
        }

        assertThat(results).hasSize(960);
    }

    private void writeFixedQuestionBank(ObjectMapper objectMapper) throws Exception {
        ObjectNode root = (ObjectNode) objectMapper.readTree(Files.readString(SOURCE));
        normalizeDifficultyFields(root);
        for (var node : root.withArray("questions")) {
            ObjectNode questionNode = (ObjectNode) node;
            if ("S0304-P4-06".equals(questionNode.path("externalId").asText())) {
                ArrayNode options = objectMapper.createArrayNode();
                options.add("계절");
                options.add("온도");
                options.add("순서");
                options.add("걱정");
                questionNode.set("options", options);
                questionNode.set("answer", objectMapper.valueToTree(List.of(3)));
                questionNode.put(
                        "explanation",
                        "양면성을 본다는 것은 장점과 걱정되는 점을 함께 보는 거예요. 비교할 때는 좋은 점과 걱정도 같이 살펴보세요."
                );
                break;
            }
        }
        root.put("generationVersion", root.path("generationVersion").asText() + "-current-criteria-fixed");
        root.put(
                "normalizationNote",
                root.path("normalizationNote").asText()
                        + " / current criteria fix: S0304-P4-06 option set updated for answer-quality validator."
        );
        Files.createDirectories(FIXED_SOURCE.getParent());
        objectMapper.writeValue(FIXED_SOURCE.toFile(), root);
    }

    private void normalizeDifficultyFields(ObjectNode root) {
        normalizeDifficultyField(root.withArray("missions"));
        normalizeDifficultyField(root.withArray("questions"));
    }

    private void normalizeDifficultyField(ArrayNode nodes) {
        for (var node : nodes) {
            if (!(node instanceof ObjectNode objectNode) || !objectNode.has("difficulty")) {
                continue;
            }
            var difficulty = objectNode.get("difficulty");
            if (!difficulty.isNumber()) {
                continue;
            }
            objectNode.put("difficulty", toDifficultyBand(difficulty.asInt()));
        }
    }

    private String toDifficultyBand(int difficulty) {
        if (difficulty <= 2) {
            return "LOW";
        }
        if (difficulty == 3) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private QuestionResult validate(QuestionValidationService validationService, AuditQuestion question) {
        QuestionValidationReport report = validationService.validate(
                new QuestionValidationService.ValidationRequest(
                        question.toStructuredQuestionSchema(),
                        List.of(),
                        List.of()
                )
        );

        return new QuestionResult(
                question.externalId(),
                question.missionCode(),
                question.type() == null ? null : question.type().name(),
                report.pass(),
                report.recommendedAction().name(),
                report.hardFailReasons(),
                report.softWarnings(),
                report.repairHints()
        );
    }

    private String renderMarkdown(FullAuditReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Current Criteria Full Validation Audit\n\n");
        sb.append("- Source: `").append(report.source()).append("`\n");
        sb.append("- Total questions: ").append(report.totalQuestions()).append('\n');
        sb.append("- Passed: ").append(report.passedQuestions()).append('\n');
        sb.append("- Failed: ").append(report.failedQuestions()).append('\n');
        sb.append("\n## Recommended Action Counts\n\n");
        report.recommendedActionCounts().forEach((reason, count) ->
                sb.append("- ").append(reason).append(": ").append(count).append('\n'));
        sb.append("\n## Hard Fail Counts\n\n");
        report.hardFailCounts().forEach((reason, count) ->
                sb.append("- ").append(reason).append(": ").append(count).append('\n'));
        sb.append("\n## Warning Counts\n\n");
        report.warningCounts().forEach((reason, count) ->
                sb.append("- ").append(reason).append(": ").append(count).append('\n'));
        sb.append("\n## Failed Question Samples\n\n");
        report.failedSamples().forEach(result -> {
            sb.append("- ").append(result.externalId())
                    .append(" / ").append(result.missionCode())
                    .append(" / ").append(result.type())
                    .append(" / action=").append(result.recommendedAction())
                    .append('\n');
            sb.append("  - hardFails: ").append(String.join(", ", result.hardFailReasons())).append('\n');
            sb.append("  - warnings: ").append(String.join(", ", result.softWarnings())).append('\n');
        });
        return sb.toString();
    }

    private record FullAuditReport(
            String source,
            int totalQuestions,
            long passedQuestions,
            int failedQuestions,
            Map<String, Long> recommendedActionCounts,
            Map<String, Long> hardFailCounts,
            Map<String, Long> warningCounts,
            List<QuestionResult> failedSamples
    ) {
    }

    private record QuestionResult(
            String externalId,
            String missionCode,
            String type,
            boolean pass,
            String recommendedAction,
            List<String> hardFailReasons,
            List<String> softWarnings,
            List<String> repairHints
    ) {
    }
}
