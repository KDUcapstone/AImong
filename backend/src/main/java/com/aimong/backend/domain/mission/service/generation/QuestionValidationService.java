package com.aimong.backend.domain.mission.service.generation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionValidationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionValidationService.class);

    private final SchemaValidator schemaValidator;
    private final SafetyValidator safetyValidator;
    private final CurriculumFitValidator curriculumFitValidator;
    private final StructureRuleValidator structureRuleValidator;
    private final ElementaryReadabilityValidator elementaryReadabilityValidator;
    private final AnswerQualityValidator answerQualityValidator;
    private final ExplanationQualityValidator explanationQualityValidator;
    private final NaturalnessValidator naturalnessValidator;
    private final KoreanSurfaceLintValidator koreanSurfaceLintValidator;
    private final Step3VocabularyCeilingValidator step3VocabularyCeilingValidator;
    private final SimilarityDeduplicator similarityDeduplicator;
    private final KerisGoldExampleRegistry kerisGoldExampleRegistry;
    private final ObjectMapper objectMapper;

    public QuestionValidationReport validate(ValidationRequest request) {
        StructuredQuestionSchema normalizedCandidate = normalizeCandidate(request.candidate());

        ValidationSubResult schemaResult = schemaValidator.validate(normalizedCandidate);
        if (!schemaResult.hardFailReasons().isEmpty()) {
            QuestionValidationReport report = buildReport(
                    normalizedCandidate,
                    schemaResult,
                    new ValidationSubResult(100, List.of(), List.of(), List.of()),
                    new CurriculumFitValidator.CurriculumFitResult(100, 100, List.of(), List.of(), List.of(), List.of()),
                    new ValidationSubResult(100, List.of(), List.of(), List.of()),
                    new ElementaryReadabilityValidator.ReadabilityResult(100, List.of(), List.of(), List.of()),
                    new AnswerQualityValidator.AnswerQualityResult(100, 100, List.of(), List.of(), List.of()),
                    new ValidationSubResult(100, List.of(), List.of(), List.of()),
                    new ValidationSubResult(100, List.of(), List.of(), List.of()),
                    new ValidationSubResult(100, List.of(), List.of(), List.of()),
                    new ValidationSubResult(100, List.of(), List.of(), List.of()),
                    SimilarityDeduplicator.SimilarityCheckResult.clean()
            );
            logReport("validation-hard-fail", report);
            return report;
        }

        ValidationSubResult safetyResult = safetyValidator.validate(normalizedCandidate);
        CurriculumFitValidator.CurriculumFitResult curriculumResult = curriculumFitValidator.validate(normalizedCandidate);
        ValidationSubResult structureResult = structureRuleValidator.validate(normalizedCandidate);
        ElementaryReadabilityValidator.ReadabilityResult readabilityResult = elementaryReadabilityValidator.validate(normalizedCandidate);
        AnswerQualityValidator.AnswerQualityResult answerResult = answerQualityValidator.validate(normalizedCandidate);
        ValidationSubResult explanationResult = explanationQualityValidator.validate(normalizedCandidate);
        ValidationSubResult naturalnessResult = naturalnessValidator.validate(normalizedCandidate);
        ValidationSubResult surfaceLintResult = koreanSurfaceLintValidator.validate(normalizedCandidate);
        ValidationSubResult step3VocabularyResult = step3VocabularyCeilingValidator.validate(normalizedCandidate);
        SimilarityDeduplicator.SimilarityCheckResult similarityResult = similarityDeduplicator.validate(
                normalizedCandidate,
                request.existingMissionPrompts(),
                mergeGoldExamples(normalizedCandidate.missionCode(), request.goldExamplePrompts())
        );

        QuestionValidationReport report = buildReport(
                normalizedCandidate,
                schemaResult,
                safetyResult,
                curriculumResult,
                structureResult,
                readabilityResult,
                answerResult,
                explanationResult,
                naturalnessResult,
                surfaceLintResult,
                step3VocabularyResult,
                similarityResult
        );
        logReport(report.pass() ? "validation-pass" : "validation-reject", report);
        return report;
    }

    private StructuredQuestionSchema normalizeCandidate(StructuredQuestionSchema candidate) {
        return new StructuredQuestionSchema(
                candidate.missionCode() == null ? "" : candidate.missionCode().trim(),
                candidate.packNo(),
                candidate.difficultyBand(),
                candidate.type(),
                candidate.question() == null ? "" : candidate.question().trim().replaceAll("\\s+", " "),
                ValidationTextUtils.normalizeOptions(candidate.options()),
                candidate.answer(),
                candidate.explanation() == null ? "" : candidate.explanation().trim().replaceAll("\\s+", " "),
                candidate.contentTags() == null ? List.of() : candidate.contentTags().stream().map(String::trim).distinct().toList(),
                candidate.curriculumRef() == null ? "" : candidate.curriculumRef().trim(),
                candidate.difficulty()
        );
    }

    private List<String> mergeGoldExamples(String missionCode, List<String> requestGoldExamples) {
        Set<String> merged = new LinkedHashSet<>(kerisGoldExampleRegistry.findQuestionPrompts(missionCode));
        merged.addAll(requestGoldExamples);
        return List.copyOf(merged);
    }

    private QuestionValidationReport buildReport(
            StructuredQuestionSchema candidate,
            ValidationSubResult schemaResult,
            ValidationSubResult safetyResult,
            CurriculumFitValidator.CurriculumFitResult curriculumResult,
            ValidationSubResult structureResult,
            ElementaryReadabilityValidator.ReadabilityResult readabilityResult,
            AnswerQualityValidator.AnswerQualityResult answerResult,
            ValidationSubResult explanationResult,
            ValidationSubResult naturalnessResult,
            ValidationSubResult surfaceLintResult,
            ValidationSubResult step3VocabularyResult,
            SimilarityDeduplicator.SimilarityCheckResult similarityResult
    ) {
        List<String> hardFails = new ArrayList<>();
        hardFails.addAll(schemaResult.hardFailReasons());
        hardFails.addAll(safetyResult.hardFailReasons());
        hardFails.addAll(curriculumResult.curriculumHardFails());
        hardFails.addAll(curriculumResult.stageHardFails());
        hardFails.addAll(structureResult.hardFailReasons());
        hardFails.addAll(readabilityResult.hardFailReasons());
        hardFails.addAll(answerResult.hardFailReasons());
        hardFails.addAll(explanationResult.hardFailReasons());
        hardFails.addAll(naturalnessResult.hardFailReasons());
        hardFails.addAll(surfaceLintResult.hardFailReasons());
        hardFails.addAll(step3VocabularyResult.hardFailReasons());
        hardFails.addAll(similarityResult.hardFailReasons());

        List<String> warnings = new ArrayList<>();
        warnings.addAll(schemaResult.softWarnings());
        warnings.addAll(safetyResult.softWarnings());
        warnings.addAll(curriculumResult.warnings());
        warnings.addAll(structureResult.softWarnings());
        warnings.addAll(readabilityResult.softWarnings());
        warnings.addAll(answerResult.softWarnings());
        warnings.addAll(explanationResult.softWarnings());
        warnings.addAll(naturalnessResult.softWarnings());
        warnings.addAll(surfaceLintResult.softWarnings());
        warnings.addAll(step3VocabularyResult.softWarnings());
        warnings.addAll(similarityResult.softWarnings());

        List<String> repairHints = new ArrayList<>();
        repairHints.addAll(schemaResult.repairHints());
        repairHints.addAll(safetyResult.repairHints());
        repairHints.addAll(curriculumResult.repairHints());
        repairHints.addAll(structureResult.repairHints());
        repairHints.addAll(readabilityResult.repairHints());
        repairHints.addAll(answerResult.repairHints());
        repairHints.addAll(explanationResult.repairHints());
        repairHints.addAll(naturalnessResult.repairHints());
        repairHints.addAll(surfaceLintResult.repairHints());
        repairHints.addAll(step3VocabularyResult.repairHints());
        repairHints.addAll(similarityResult.repairHints());
        repairHints = repairHints.stream().distinct().toList();

        int criteriaFit = average(
                curriculumResult.curriculumScore(),
                answerResult.answerClarityScore(),
                answerResult.distractorQualityScore(),
                explanationResult.score()
        );
        int kerisThreeStepFit = average(curriculumResult.curriculumScore(), curriculumResult.stageScore());
        int elementaryDifficultyDirection = average(readabilityResult.score(), curriculumResult.stageScore());
        int structureQuotaTypeRules = average(schemaResult.score(), structureResult.score());
        int duplicationOriginality = similarityResult.originalityScore();
        int naturalnessScore = average(
                naturalnessResult.score(),
                surfaceLintResult.score(),
                step3VocabularyResult.score()
        );
        int productionReadiness = hardFails.isEmpty()
                ? average(
                        safetyResult.score(),
                        criteriaFit,
                        kerisThreeStepFit,
                        elementaryDifficultyDirection,
                        structureQuotaTypeRules,
                        naturalnessScore,
                        duplicationOriginality
                )
                : 0;

        QuestionValidationScores scores = new QuestionValidationScores(
                schemaResult.score(),
                safetyResult.score(),
                curriculumResult.curriculumScore(),
                curriculumResult.stageScore(),
                readabilityResult.score(),
                answerResult.answerClarityScore(),
                answerResult.distractorQualityScore(),
                explanationResult.score(),
                similarityResult.originalityScore(),
                criteriaFit,
                kerisThreeStepFit,
                elementaryDifficultyDirection,
                structureQuotaTypeRules,
                naturalnessScore,
                duplicationOriginality,
                productionReadiness,
                overallScore(
                        schemaResult.score(),
                        safetyResult.score(),
                        curriculumResult.curriculumScore(),
                        curriculumResult.stageScore(),
                        structureResult.score(),
                        readabilityResult.score(),
                        answerResult.answerClarityScore(),
                        answerResult.distractorQualityScore(),
                        explanationResult.score(),
                        naturalnessScore,
                        similarityResult.originalityScore()
                )
        );

        ValidationDecision decision = decide(candidate, hardFails, scores, similarityResult.escalateSuggested());
        boolean pass = hardFails.isEmpty() && decision == ValidationDecision.SAVE;

        return new QuestionValidationReport(
                pass,
                decision,
                List.copyOf(hardFails),
                List.copyOf(warnings),
                scores,
                new NormalizedQuestionView(
                        candidate.missionCode(),
                        candidate.type() == null ? null : candidate.type().name(),
                        candidate.question(),
                        candidate.options(),
                        candidate.answer(),
                        candidate.explanation(),
                        candidate.contentTags(),
                        candidate.difficulty()
                ),
                repairHints
        );
    }

    private int overallScore(int... values) {
        return (int) Math.round(java.util.Arrays.stream(values).average().orElse(0d));
    }

    private int average(int... values) {
        return (int) Math.round(java.util.Arrays.stream(values).average().orElse(0d));
    }

    private ValidationDecision decide(
            StructuredQuestionSchema candidate,
            List<String> hardFails,
            QuestionValidationScores scores,
            boolean escalateSuggested
    ) {
        if (!hardFails.isEmpty()) {
            if (candidate.difficulty() >= 4 || escalateSuggested) {
                return ValidationDecision.ESCALATE;
            }
            if (scores.criteriaFit() < 85
                    || scores.kerisThreeStepFit() < 90
                    || scores.elementaryDifficultyDirection() < 80
                    || scores.structureQuotaTypeRules() < 90) {
                return ValidationDecision.REWRITE;
            }
            return ValidationDecision.RETRY;
        }
        if (scores.criteriaFit() >= 85
                && scores.kerisThreeStepFit() >= 90
                && scores.elementaryDifficultyDirection() >= 80
                && scores.structureQuotaTypeRules() >= 90
                && scores.naturalness() >= 80
                && scores.answerClarity() >= 85
                && scores.explanationQuality() >= 80
                && scores.duplicationOriginality() >= 75
                && scores.productionReadiness() >= 85) {
            return ValidationDecision.SAVE;
        }
        if (candidate.difficulty() >= 4 && scores.duplicationOriginality() < 75) {
            return ValidationDecision.ESCALATE;
        }
        if (scores.criteriaFit() < 85
                || scores.kerisThreeStepFit() < 90
                || scores.elementaryDifficultyDirection() < 80
                || scores.structureQuotaTypeRules() < 90
                || scores.naturalness() < 80) {
            return ValidationDecision.REWRITE;
        }
        return ValidationDecision.RETRY;
    }

    private void logReport(String prefix, QuestionValidationReport report) {
        try {
            log.info("{} {}", prefix, objectMapper.writeValueAsString(report));
        } catch (JsonProcessingException exception) {
            log.warn("{} {}", prefix, report);
        }
    }

    public record ValidationRequest(
            StructuredQuestionSchema candidate,
            List<String> existingMissionPrompts,
            List<String> goldExamplePrompts
    ) {
    }
}
