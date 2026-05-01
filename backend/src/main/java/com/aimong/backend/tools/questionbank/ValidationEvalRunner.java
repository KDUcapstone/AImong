package com.aimong.backend.tools.questionbank;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ValidationEvalRunner {

    private ValidationEvalRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            throw new IllegalArgumentException(
                    "usage: ValidationEvalRunner <serve-json> <v2-json> <synthetic-json> <audit-md> <audit-json> <serve-report-base>"
            );
        }

        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        QuestionBankAuditLoader loader = new QuestionBankAuditLoader(objectMapper);
        AuditQuestionBank serveBank = loader.load(Path.of(args[0]));
        AuditQuestionBank v2 = loader.load(Path.of(args[1]));
        SyntheticValidationFixtureSet synthetic = objectMapper.readValue(
                Files.readString(Path.of(args[2])),
                SyntheticValidationFixtureSet.class
        );

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

        ValidationBatchSummary serveSummary = evaluateBank(serveBank);
        SyntheticAuditSummary syntheticSummary = evaluateSynthetic(validationService, synthetic);
        AuditReport auditReport = buildAuditReport(serveSummary, syntheticSummary, v2, serveBank);

        writeJson(objectMapper, Path.of(args[4]), auditReport);
        writeText(Path.of(args[3]), renderAuditMarkdown(auditReport));

        Path serveBase = Path.of(args[5]);
        writeJson(objectMapper, replaceExtension(serveBase, ".json"), serveSummary);
        writeText(replaceExtension(serveBase, ".md"), renderBatchMarkdown(serveSummary));
    }

    private static ValidationBatchSummary evaluateBank(AuditQuestionBank bank) {
        CoreQuestionDiversityValidator.CoreQuestionDiversityReport diversity =
                new CoreQuestionDiversityValidator().validate(bank.questions());
        OptionLengthBiasValidator.OptionLengthBiasReport optionBias =
                new OptionLengthBiasValidator().validate(bank.questions());
        AnswerIndexBalanceValidator.AnswerIndexBalanceReport answerIndex =
                new AnswerIndexBalanceValidator().validate(bank.questions());
        ExplanationVariationValidator.ExplanationVariationReport explanationVariation =
                new ExplanationVariationValidator().validate(bank.questions());
        BatchSurfaceLintAnalyzer.BatchSurfaceLintReport surfaceLint =
                new BatchSurfaceLintAnalyzer().validate(bank.questions());
        Step3VocabularyCeilingAnalyzer.Step3VocabularyReport step3Vocabulary =
                new Step3VocabularyCeilingAnalyzer().validate(bank.questions());

        Map<String, Long> perMissionCounts = bank.questions().stream()
                .collect(Collectors.groupingBy(AuditQuestion::missionCode, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> perPackCounts = bank.questions().stream()
                .filter(question -> question.packNo() != null)
                .collect(Collectors.groupingBy(
                        question -> question.missionCode() + "-P" + question.packNo(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> typeDistribution = bank.questions().stream()
                .collect(Collectors.groupingBy(question -> question.type().name(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> bandDistribution = bank.questions().stream()
                .filter(question -> question.difficultyBand() != null)
                .collect(Collectors.groupingBy(question -> question.difficultyBand().name(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> stageDistribution = bank.questions().stream()
                .collect(Collectors.groupingBy(question -> String.valueOf(question.stage()), LinkedHashMap::new, Collectors.counting()));

        double longestRatio = optionBias.evaluatedCount() == 0
                ? 0d
                : optionBias.correctOptionUniqueLongestCount() / (double) optionBias.evaluatedCount();
        double strongBiasRatio = optionBias.evaluatedCount() == 0
                ? 0d
                : optionBias.strongOptionLengthBiasCount() / (double) optionBias.evaluatedCount();

        double multipleRatio = answerIndex.multipleMaxMinRatio();
        double situationRatio = answerIndex.situationMaxMinRatio();

        List<String> topRisks = new ArrayList<>();
        if (diversity.identicalSixPackSlotCount() > 0) {
            topRisks.add("Identical 6-pack slots detected: " + diversity.identicalSixPackSlotCount());
        }
        if (strongBiasRatio > 0.02d) {
            topRisks.add("Strong option-length bias ratio exceeds target: " + strongBiasRatio);
        }
        if (longestRatio > 0.40d) {
            topRisks.add("Correct option unique-longest ratio exceeds target: " + longestRatio);
        }
        if (multipleRatio > 1.4d) {
            topRisks.add("MULTIPLE answer-index imbalance ratio exceeds target: " + multipleRatio);
        }
        if (situationRatio > 1.4d) {
            topRisks.add("SITUATION answer-index imbalance ratio exceeds target: " + situationRatio);
        }
        if (surfaceLint.hitCount() > 0) {
            topRisks.add("Surface lint hits detected: " + surfaceLint.hitCount());
        }
        if (step3Vocabulary.hitCount() > 0) {
            topRisks.add("Flagged Step 3 vocabulary hits: " + step3Vocabulary.hitCount());
        }
        if (explanationVariation.repeatedExplanationSuffixPatternCount() > 0) {
            topRisks.add("Repeated explanation suffix patterns detected: "
                    + explanationVariation.repeatedExplanationSuffixPatternCount());
        }
        if (diversity.sameMissionNearDuplicateRate() > 0.02d) {
            topRisks.add("Same-mission near-duplicate rate is elevated: " + diversity.sameMissionNearDuplicateRate());
        }
        if (optionBias.answerOptionStyleImbalanceWarnings() > 0) {
            topRisks.add("Answer option style imbalance warnings: " + optionBias.answerOptionStyleImbalanceWarnings());
        }

        return new ValidationBatchSummary(
                bank.totalQuestionCount(),
                bank.totalMissionCount(),
                perMissionCounts,
                perPackCounts,
                typeDistribution,
                bandDistribution,
                stageDistribution,
                diversity,
                optionBias,
                longestRatio,
                strongBiasRatio,
                answerIndex,
                surfaceLint,
                explanationVariation,
                step3Vocabulary,
                verdict(
                        diversity.identicalSixPackSlotCount(),
                        strongBiasRatio,
                        surfaceLint.hitCount(),
                        step3Vocabulary.hitCount(),
                        longestRatio,
                        multipleRatio,
                        situationRatio
                ),
                topRisks.stream().limit(10).toList()
        );
    }

    private static SyntheticAuditSummary evaluateSynthetic(
            QuestionValidationService validationService,
            SyntheticValidationFixtureSet fixtureSet
    ) {
        List<SyntheticAuditCaseResult> results = new ArrayList<>();
        int totalHardExpectations = 0;
        int matchedHardExpectations = 0;
        int totalWarningExpectations = 0;
        int matchedWarningExpectations = 0;
        int totalBatchExpectations = 0;
        int matchedBatchExpectations = 0;

        for (SyntheticValidationFixture fixture : fixtureSet.cases()) {
            List<String> actualBatchSignals = List.of();
            List<String> expectedHardFails = fixture.expectedHardFails() == null ? List.of() : fixture.expectedHardFails();
            List<String> expectedWarnings = fixture.expectedWarnings() == null ? List.of() : fixture.expectedWarnings();
            List<String> expectedBatchSignals =
                    fixture.expectedBatchSignals() == null ? List.of() : fixture.expectedBatchSignals();
            QuestionValidationReport report = fixture.candidate() == null
                    ? new QuestionValidationReport(
                            false,
                            com.aimong.backend.domain.mission.service.generation.ValidationDecision.RETRY,
                            List.of(),
                            List.of(),
                            null,
                            null,
                            List.of()
                    )
                    : validationService.validate(
                            new QuestionValidationService.ValidationRequest(
                                    fixture.candidate().toStructuredQuestionSchema(),
                                    List.of(),
                                    List.of()
                            )
                    );
            if (fixture.isBatchCase()) {
                actualBatchSignals = collectBatchSignals(fixture.candidates());
            }
            int hardMatches = 0;
            for (String expected : expectedHardFails) {
                totalHardExpectations++;
                if (report.hardFailReasons().stream().anyMatch(reason -> reason.contains(expected))) {
                    hardMatches++;
                    matchedHardExpectations++;
                }
            }
            int warningMatches = 0;
            for (String expected : expectedWarnings) {
                totalWarningExpectations++;
                if (report.softWarnings().stream().anyMatch(reason -> reason.contains(expected))) {
                    warningMatches++;
                    matchedWarningExpectations++;
                }
            }
            int batchMatches = 0;
            for (String expected : expectedBatchSignals) {
                totalBatchExpectations++;
                if (actualBatchSignals.stream().anyMatch(signal -> signal.contains(expected))) {
                    batchMatches++;
                    matchedBatchExpectations++;
                }
            }
            results.add(new SyntheticAuditCaseResult(
                    fixture.id(),
                    fixture.category(),
                    hardMatches,
                    expectedHardFails.size(),
                    warningMatches,
                    expectedWarnings.size(),
                    batchMatches,
                    expectedBatchSignals.size(),
                    report.hardFailReasons(),
                    report.softWarnings(),
                    actualBatchSignals
            ));
        }

        return new SyntheticAuditSummary(
                results,
                percentage(matchedHardExpectations, totalHardExpectations),
                percentage(matchedWarningExpectations, totalWarningExpectations),
                percentage(matchedBatchExpectations, totalBatchExpectations)
        );
    }

    private static AuditReport buildAuditReport(
            ValidationBatchSummary serveSummary,
            SyntheticAuditSummary syntheticSummary,
            AuditQuestionBank v2,
            AuditQuestionBank serveBank
    ) {
        List<String> strengths = List.of(
                "Schema/type/tag/safety checks run before persistence through QuestionValidationService and GeneratedQuestionPersistenceService.",
                "missionId-based serving contract and 10-question response shape are preserved in controller/service code and API tests.",
                "Near-duplicate prompt rejection already exists at mission scope through SimilarityDeduplicator."
        );
        List<String> gapsClosed = List.of(
                "Added KoreanSurfaceLintValidator for obvious Korean surface errors.",
                "Added Step3VocabularyCeilingValidator to flag over-advanced Step 3 wording.",
                "Added whole-bank diversity, option-bias, answer-index, explanation-variation, and batch lint evaluators.",
                "Added synthetic negative fixtures so validator behavior is tested against known failure modes."
        );

        String validatorAdequacyVerdict = "PASS";
        if (syntheticSummary.hardFailDetectionRate() < 100d
                || syntheticSummary.warningDetectionRate() < 90d
                || syntheticSummary.batchSignalDetectionRate() < 95d) {
            validatorAdequacyVerdict = "WARN";
        }
        if (syntheticSummary.hardFailDetectionRate() < 95d
                || syntheticSummary.batchSignalDetectionRate() < 90d) {
            validatorAdequacyVerdict = "FAIL";
        }

        String serveBankQualityVerdict = serveSummary.verdict();
        String overallVerdict = "PASS";
        if (!"PASS".equals(validatorAdequacyVerdict) || !"PASS".equals(serveBankQualityVerdict)) {
            overallVerdict = "WARN";
        }
        if ("FAIL".equals(validatorAdequacyVerdict) || "FAIL".equals(serveBankQualityVerdict)) {
            overallVerdict = "FAIL";
        }

        return new AuditReport(
                overallVerdict,
                validatorAdequacyVerdict,
                serveBankQualityVerdict,
                strengths,
                gapsClosed,
                List.of(
                        "Validator thresholds:",
                        "  - Hard validation detection target = 100%",
                        "  - Warning detection target = 90%+",
                        "  - Batch signal detection target = 95%+",
                        "Serve-bank targets:",
                        "  - Identical 6-pack slot = 0",
                        "  - Strong option-length bias ratio <= 2%",
                        "  - Correct option unique-longest ratio <= 40%",
                        "  - MULTIPLE answer-index max/min ratio <= 1.4",
                        "  - SITUATION answer-index max/min ratio <= 1.4",
                        "  - Korean surface lint hits = 0",
                        "Synthetic hard-fail detection rate: " + syntheticSummary.hardFailDetectionRate(),
                        "Synthetic warning detection rate: " + syntheticSummary.warningDetectionRate(),
                        "Synthetic batch-signal detection rate: " + syntheticSummary.batchSignalDetectionRate(),
                        "Serve-bank identical 6-pack slot count: " + serveSummary.diversity().identicalSixPackSlotCount(),
                        "Serve-bank strong option-length-bias count: " + serveSummary.optionBias().strongOptionLengthBiasCount(),
                        "Serve-bank strong option-length-bias ratio: " + serveSummary.strongOptionLengthBiasRatio(),
                        "Serve-bank correct option unique-longest ratio: " + serveSummary.correctOptionUniqueLongestRatio(),
                        "Serve-bank MULTIPLE answer-index max/min ratio: " + serveSummary.answerIndexBalance().multipleMaxMinRatio(),
                        "Serve-bank SITUATION answer-index max/min ratio: " + serveSummary.answerIndexBalance().situationMaxMinRatio(),
                        "Serve-bank surface lint hits: " + serveSummary.surfaceLint().hitCount(),
                        "Serve-bank Step 3 vocabulary hits: " + serveSummary.step3Vocabulary().hitCount(),
                        "Baseline v2 question count: " + v2.totalQuestionCount(),
                        "Serve-bank question count: " + serveBank.totalQuestionCount()
                ),
                serveSummary.topRisks(),
                syntheticSummary
        );
    }

    private static String renderAuditMarkdown(AuditReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Validation Audit Report\n\n");
        sb.append("- Overall Verdict: ").append(report.verdict()).append('\n');
        sb.append("- Validator Adequacy Verdict: ").append(report.validatorAdequacyVerdict()).append('\n');
        sb.append("- Serve-Bank Quality Verdict: ").append(report.serveBankQualityVerdict()).append('\n');
        sb.append("- Strengths:\n");
        report.strengths().forEach(value -> sb.append("  - ").append(value).append('\n'));
        sb.append("- Gaps Closed:\n");
        report.gapsClosed().forEach(value -> sb.append("  - ").append(value).append('\n'));
        sb.append("- Evidence:\n");
        report.evidence().forEach(value -> sb.append("  - ").append(value).append('\n'));
        sb.append("- Top Risks:\n");
        report.topRisks().forEach(value -> sb.append("  - ").append(value).append('\n'));
        return sb.toString();
    }

    private static String renderBatchMarkdown(ValidationBatchSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Question Bank v4 Batch Report\n\n");
        sb.append("- Verdict: ").append(summary.verdict()).append('\n');
        sb.append("- totalQuestionCount: ").append(summary.totalQuestionCount()).append('\n');
        sb.append("- missionCount: ").append(summary.missionCount()).append('\n');
        sb.append("- identical6PackSlotCount: ").append(summary.diversity().identicalSixPackSlotCount()).append('\n');
        sb.append("- sameMissionNearDuplicateRate: ").append(summary.diversity().sameMissionNearDuplicateRate()).append('\n');
        sb.append("- strongOptionLengthBiasCount: ").append(summary.optionBias().strongOptionLengthBiasCount()).append('\n');
        sb.append("- strongOptionLengthBiasRatio: ").append(summary.strongOptionLengthBiasRatio()).append('\n');
        sb.append("- correctOptionUniqueLongestCount: ").append(summary.optionBias().correctOptionUniqueLongestCount()).append('\n');
        sb.append("- correctOptionUniqueLongestRatio: ").append(summary.correctOptionUniqueLongestRatio()).append('\n');
        sb.append("- answerOptionStyleImbalanceWarnings: ").append(summary.optionBias().answerOptionStyleImbalanceWarnings()).append('\n');
        sb.append("- multipleAnswerIndexMaxMinRatio: ").append(summary.answerIndexBalance().multipleMaxMinRatio()).append('\n');
        sb.append("- situationAnswerIndexMaxMinRatio: ").append(summary.answerIndexBalance().situationMaxMinRatio()).append('\n');
        sb.append("- koreanSurfaceLintHits: ").append(summary.surfaceLint().hitCount()).append('\n');
        sb.append("- repeatedExplanationSuffixPatternCount: ")
                .append(summary.explanationVariation().repeatedExplanationSuffixPatternCount()).append('\n');
        sb.append("- mostFrequentExplanationEndings:\n");
        summary.explanationVariation().overusedExplanationEndings()
                .forEach(value -> sb.append("  - ").append(value).append('\n'));
        sb.append("- flaggedStep3VocabularyCount: ").append(summary.step3Vocabulary().hitCount()).append('\n');
        sb.append("- slotLevelRepetitionHotspots:\n");
        summary.diversity().slotLevelRepetitionHotspots()
                .forEach((key, value) -> sb.append("  - ").append(key).append(": ").append(value).append('\n'));
        sb.append("- Top Risks:\n");
        summary.topRisks().forEach(value -> sb.append("  - ").append(value).append('\n'));
        return sb.toString();
    }

    private static Path replaceExtension(Path path, String extension) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot >= 0 ? fileName.substring(0, dot) : fileName;
        return path.resolveSibling(baseName + extension);
    }

    private static void writeJson(ObjectMapper objectMapper, Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, objectMapper.writeValueAsString(value));
    }

    private static void writeText(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }

    private static List<String> collectBatchSignals(List<AuditQuestion> questions) {
        List<String> signals = new ArrayList<>();
        CoreQuestionDiversityValidator.CoreQuestionDiversityReport diversity =
                new CoreQuestionDiversityValidator().validate(questions);
        OptionLengthBiasValidator.OptionLengthBiasReport optionBias =
                new OptionLengthBiasValidator().validate(questions);
        AnswerIndexBalanceValidator.AnswerIndexBalanceReport answerIndex =
                new AnswerIndexBalanceValidator().validate(questions);
        ExplanationVariationValidator.ExplanationVariationReport explanationVariation =
                new ExplanationVariationValidator().validate(questions);
        BatchSurfaceLintAnalyzer.BatchSurfaceLintReport surfaceLint =
                new BatchSurfaceLintAnalyzer().validate(questions);
        Step3VocabularyCeilingAnalyzer.Step3VocabularyReport step3Vocabulary =
                new Step3VocabularyCeilingAnalyzer().validate(questions);

        if (diversity.identicalSixPackSlotCount() > 0) {
            signals.add("batch.identical_six_pack_slot");
        }
        if (diversity.sameMissionNearDuplicateRate() > 0d) {
            signals.add("batch.same_mission_near_duplicate");
        }
        if (optionBias.correctOptionUniqueLongestCount() > 0) {
            signals.add("batch.correct_option_unique_longest");
        }
        if (optionBias.strongOptionLengthBiasCount() > 0) {
            signals.add("batch.strong_option_length_bias");
        }
        if (optionBias.answerOptionStyleImbalanceWarnings() > 0) {
            signals.add("batch.answer_option_style_imbalance");
        }
        if (answerIndex.multipleMaxMinRatio() > 1.4d
                || answerIndex.situationMaxMinRatio() > 1.4d
                || answerIndex.combinedMaxMinRatio() > 1.4d) {
            signals.add("batch.answer_index_imbalance");
        }
        if (explanationVariation.repeatedExplanationSuffixPatternCount() > 0) {
            signals.add("batch.explanation_suffix_repetition");
        }
        if (surfaceLint.hitCount() > 0) {
            signals.add("batch.surface_lint_hit");
        }
        if (step3Vocabulary.hitCount() > 0) {
            signals.add("batch.step3_vocabulary_hit");
        }
        return signals;
    }

    private static String verdict(
            int identicalSixPackSlotCount,
            double strongBiasRatio,
            int surfaceHits,
            int step3Hits,
            double longestRatio,
            double multipleRatio,
            double situationRatio
    ) {
        if (identicalSixPackSlotCount > 0 || surfaceHits > 0) {
            return "FAIL";
        }
        if (strongBiasRatio > 0.02d
                || step3Hits > 0
                || longestRatio > 0.40d
                || multipleRatio > 1.4d
                || situationRatio > 1.4d) {
            return "WARN";
        }
        return "PASS";
    }

    private static double percentage(int matched, int total) {
        if (total == 0) {
            return 100d;
        }
        return Math.round((matched * 10000d) / total) / 100d;
    }

    public record ValidationBatchSummary(
            int totalQuestionCount,
            int missionCount,
            Map<String, Long> perMissionQuestionCount,
            Map<String, Long> perPackQuestionCount,
            Map<String, Long> typeDistribution,
            Map<String, Long> difficultyBandDistribution,
            Map<String, Long> stageDistribution,
            CoreQuestionDiversityValidator.CoreQuestionDiversityReport diversity,
            OptionLengthBiasValidator.OptionLengthBiasReport optionBias,
            double correctOptionUniqueLongestRatio,
            double strongOptionLengthBiasRatio,
            AnswerIndexBalanceValidator.AnswerIndexBalanceReport answerIndexBalance,
            BatchSurfaceLintAnalyzer.BatchSurfaceLintReport surfaceLint,
            ExplanationVariationValidator.ExplanationVariationReport explanationVariation,
            Step3VocabularyCeilingAnalyzer.Step3VocabularyReport step3Vocabulary,
            String verdict,
            List<String> topRisks
    ) {
    }

    public record SyntheticAuditCaseResult(
            String id,
            String category,
            int matchedHardFails,
            int expectedHardFails,
            int matchedWarnings,
            int expectedWarnings,
            int matchedBatchSignals,
            int expectedBatchSignals,
            List<String> actualHardFails,
            List<String> actualWarnings,
            List<String> actualBatchSignals
    ) {
    }

    public record SyntheticAuditSummary(
            List<SyntheticAuditCaseResult> cases,
            double hardFailDetectionRate,
            double warningDetectionRate,
            double batchSignalDetectionRate
    ) {
    }

    public record AuditReport(
            String verdict,
            String validatorAdequacyVerdict,
            String serveBankQualityVerdict,
            List<String> strengths,
            List<String> gapsClosed,
            List<String> evidence,
            List<String> topRisks,
            SyntheticAuditSummary syntheticSummary
    ) {
    }
}
