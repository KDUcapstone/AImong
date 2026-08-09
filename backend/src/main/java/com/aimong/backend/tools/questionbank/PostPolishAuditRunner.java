package com.aimong.backend.tools.questionbank;

import java.util.List;

public final class PostPolishAuditRunner {

    public PolishReport compare(
            AuditQuestionBank before,
            AuditQuestionBank after,
            PolishingTargetExtractor.TargetReport beforeTargets,
            PolishingTargetExtractor.TargetReport afterTargets
    ) {
        BatchMetrics beforeMetrics = evaluate(before);
        BatchMetrics afterMetrics = evaluate(after);

        boolean passMaintained = "PASS".equals(afterMetrics.verdict())
                && afterMetrics.strongOptionLengthBiasCount() <= beforeMetrics.strongOptionLengthBiasCount()
                && afterMetrics.multipleAnswerIndexMaxMinRatio() == beforeMetrics.multipleAnswerIndexMaxMinRatio()
                && afterMetrics.situationAnswerIndexMaxMinRatio() == beforeMetrics.situationAnswerIndexMaxMinRatio()
                && afterMetrics.koreanSurfaceLintHits() == 0
                && afterMetrics.step3VocabularyHits() == 0;

        List<String> changedMetrics = List.of(
                "repeatedExplanationSuffixPatterns: %d -> %d".formatted(
                        beforeMetrics.repeatedExplanationSuffixPatternCount(),
                        afterMetrics.repeatedExplanationSuffixPatternCount()
                ),
                "answerOptionStyleImbalanceWarnings: %d -> %d".formatted(
                        beforeMetrics.answerOptionStyleImbalanceWarnings(),
                        afterMetrics.answerOptionStyleImbalanceWarnings()
                ),
                "correctOptionUniqueLongestRatio: %.6f -> %.6f".formatted(
                        beforeMetrics.correctOptionUniqueLongestRatio(),
                        afterMetrics.correctOptionUniqueLongestRatio()
                ),
                "strongOptionLengthBiasCount: %d -> %d".formatted(
                        beforeMetrics.strongOptionLengthBiasCount(),
                        afterMetrics.strongOptionLengthBiasCount()
                )
        );

        return new PolishReport(
                passMaintained ? "PASS" : "FAIL",
                beforeMetrics,
                afterMetrics,
                beforeTargets.explanationClusters().size(),
                afterTargets.explanationClusters().size(),
                beforeTargets.optionStyleTargets().size(),
                afterTargets.optionStyleTargets().size(),
                changedMetrics
        );
    }

    public BatchMetrics evaluate(AuditQuestionBank bank) {
        PolishingTargetExtractor.TargetReport targets = new PolishingTargetExtractor().extract(bank);
        CoreQuestionDiversityValidator.CoreQuestionDiversityReport diversity =
                new CoreQuestionDiversityValidator().validate(bank.questions());
        OptionLengthBiasValidator.OptionLengthBiasReport optionBias =
                new OptionLengthBiasValidator().validate(bank.questions());
        AnswerIndexBalanceValidator.AnswerIndexBalanceReport answerIndex =
                new AnswerIndexBalanceValidator().validate(bank.questions());
        BatchSurfaceLintAnalyzer.BatchSurfaceLintReport surfaceLint =
                new BatchSurfaceLintAnalyzer().validate(bank.questions());
        Step3VocabularyCeilingAnalyzer.Step3VocabularyReport step3Vocabulary =
                new Step3VocabularyCeilingAnalyzer().validate(bank.questions());

        double correctOptionUniqueLongestRatio = optionBias.evaluatedCount() == 0
                ? 0d
                : optionBias.correctOptionUniqueLongestCount() / (double) optionBias.evaluatedCount();
        double strongOptionLengthBiasRatio = optionBias.evaluatedCount() == 0
                ? 0d
                : optionBias.strongOptionLengthBiasCount() / (double) optionBias.evaluatedCount();

        String verdict = "PASS";
        if (diversity.identicalSixPackSlotCount() > 0 || surfaceLint.hitCount() > 0) {
            verdict = "FAIL";
        } else if (strongOptionLengthBiasRatio > 0.02d
                || correctOptionUniqueLongestRatio > 0.40d
                || answerIndex.multipleMaxMinRatio() > 1.4d
                || answerIndex.situationMaxMinRatio() > 1.4d
                || step3Vocabulary.hitCount() > 0) {
            verdict = "WARN";
        }

        return new BatchMetrics(
                verdict,
                diversity.identicalSixPackSlotCount(),
                optionBias.strongOptionLengthBiasCount(),
                correctOptionUniqueLongestRatio,
                strongOptionLengthBiasRatio,
                answerIndex.multipleMaxMinRatio(),
                answerIndex.situationMaxMinRatio(),
                surfaceLint.hitCount(),
                step3Vocabulary.hitCount(),
                targets.explanationClusters().size(),
                optionBias.answerOptionStyleImbalanceWarnings()
        );
    }

    public record BatchMetrics(
            String verdict,
            int identicalSixPackSlotCount,
            int strongOptionLengthBiasCount,
            double correctOptionUniqueLongestRatio,
            double strongOptionLengthBiasRatio,
            double multipleAnswerIndexMaxMinRatio,
            double situationAnswerIndexMaxMinRatio,
            int koreanSurfaceLintHits,
            int step3VocabularyHits,
            int repeatedExplanationSuffixPatternCount,
            int answerOptionStyleImbalanceWarnings
    ) {
    }

    public record PolishReport(
            String verdict,
            BatchMetrics before,
            BatchMetrics after,
            int explanationClusterCountBefore,
            int explanationClusterCountAfter,
            int optionStyleWarningCountBefore,
            int optionStyleWarningCountAfter,
            List<String> changedMetrics
    ) {
    }
}
