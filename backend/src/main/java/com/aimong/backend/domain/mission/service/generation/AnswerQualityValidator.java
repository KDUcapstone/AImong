package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AnswerQualityValidator {

    public AnswerQualityResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        validateAnswerReference(candidate, hardFails, repairHints);

        if (candidate.type() == QuestionType.MULTIPLE
                || candidate.type() == QuestionType.SITUATION
                || candidate.type() == QuestionType.FILL) {
            validateOptions(candidate, hardFails, warnings, repairHints);
        }

        if (candidate.type() != null && candidate.explanation() != null && candidate.options() != null && !candidate.options().isEmpty()) {
            Integer answerIndex = answerIndex(candidate);
            if (answerIndex != null && answerIndex >= 0 && answerIndex < candidate.options().size()) {
                String normalizedExplanation = ValidationTextUtils.normalize(candidate.explanation());
                String correctOption = ValidationTextUtils.normalize(candidate.options().get(answerIndex));
                for (int index = 0; index < candidate.options().size(); index++) {
                    String option = ValidationTextUtils.normalize(candidate.options().get(index));
                    if (index != answerIndex && !option.isBlank() && normalizedExplanation.contains(option)) {
                        hardFails.add("answer.explanation_conflicts_with_answer");
                        repairHints.add("Rewrite the explanation so it clearly supports only the correct answer.");
                        break;
                    }
                }
                if (!correctOption.isBlank() && normalizedExplanation.equals(correctOption)) {
                    warnings.add("answer.explanation_repeats_answer_only");
                }
                if (ValidationTextUtils.tokenJaccard(candidate.question(), candidate.options().get(answerIndex)) >= 0.92d) {
                    warnings.add("answer.correct_option_copied_from_stem");
                    repairHints.add("Avoid making the answer obvious by copying the stem wording directly into the correct option.");
                }
                if (ValidationTextUtils.tokenJaccard(candidate.explanation(), candidate.options().get(answerIndex)) < 0.08d) {
                    warnings.add("answer.explanation_support_is_weak");
                    repairHints.add("Explain why the correct option is right using evidence from the situation or rule.");
                }
            }
        }

        int clarityScore = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 10) : 0;
        int distractorScore = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 12) : 0;
        return new AnswerQualityResult(
                clarityScore,
                distractorScore,
                List.copyOf(hardFails),
                List.copyOf(warnings),
                List.copyOf(repairHints)
        );
    }

    private void validateOptions(
            StructuredQuestionSchema candidate,
            List<String> hardFails,
            List<String> warnings,
            List<String> repairHints
    ) {
        List<String> options = candidate.options();
        Integer answerIndex = answerIndex(candidate);
        if (answerIndex == null || answerIndex < 0 || answerIndex >= options.size()) {
            hardFails.add("answer.invalid_answer_index");
            repairHints.add("Set exactly one valid answer index inside the option range.");
            return;
        }

        Set<String> uniqueFingerprints = new LinkedHashSet<>();
        for (String option : options) {
            String fingerprint = fingerprint(option);
            if (!uniqueFingerprints.add(fingerprint)) {
                hardFails.add("answer.duplicate_options");
                repairHints.add("All answer options must be meaningfully distinct.");
                return;
            }
        }

        int correctLength = options.get(answerIndex).length();
        double avgOtherLength = options.stream()
                .filter(option -> !option.equals(options.get(answerIndex)))
                .mapToInt(String::length)
                .average()
                .orElse(correctLength);
        if (avgOtherLength > 0 && correctLength > avgOtherLength * 1.8d) {
            warnings.add("answer.correct_option_too_obvious");
            repairHints.add("Balance the correct option length with the distractors.");
        }
        if (avgOtherLength > 0 && correctLength < avgOtherLength * 0.45d) {
            warnings.add("answer.correct_option_too_short");
            repairHints.add("Keep the correct option similar in detail and tone to the distractors.");
        }

        for (int left = 0; left < options.size(); left++) {
            for (int right = left + 1; right < options.size(); right++) {
                double similarity = ValidationTextUtils.tokenJaccard(options.get(left), options.get(right));
                if (similarity >= 0.92d) {
                    hardFails.add("answer.multiple_options_mean_too_similar");
                    repairHints.add("Make distractors distinguishable and avoid near-identical options.");
                    return;
                }
            }
        }

        String correct = options.get(answerIndex);
        for (int index = 0; index < options.size(); index++) {
            if (index == answerIndex) {
                continue;
            }
            double similarityToCorrect = ValidationTextUtils.tokenJaccard(correct, options.get(index));
            if (similarityToCorrect >= 0.78d
                    || compact(ValidationTextUtils.normalize(correct)).equals(compact(ValidationTextUtils.normalize(options.get(index))))) {
                hardFails.add("answer.correct_and_distractor_too_similar");
                repairHints.add("Change the distractor so only one option can reasonably count as correct.");
                return;
            }
        }

        long stemEchoCount = options.stream()
                .filter(option -> ValidationTextUtils.tokenJaccard(candidate.question(), option) >= 0.50d
                        || compact(ValidationTextUtils.normalize(candidate.question())).contains(compact(ValidationTextUtils.normalize(option))))
                .count();
        if (stemEchoCount >= 2) {
            hardFails.add("answer.multiple_options_match_stem");
            repairHints.add("Avoid writing two or more options that can both be justified directly from the stem.");
        }
    }

    private void validateAnswerReference(
            StructuredQuestionSchema candidate,
            List<String> hardFails,
            List<String> repairHints
    ) {
        if (candidate.type() == QuestionType.OX && !(candidate.answer() instanceof Boolean)) {
            hardFails.add("answer.invalid_ox_answer");
            repairHints.add("OX questions must have a boolean answer.");
        }
        if ((candidate.type() == QuestionType.MULTIPLE || candidate.type() == QuestionType.SITUATION)
                && !(candidate.answer() instanceof Integer)) {
            hardFails.add("answer.invalid_single_choice_answer");
            repairHints.add("Single-choice questions must point to exactly one option index.");
        }
        if (candidate.type() == QuestionType.FILL) {
            if (!(candidate.answer() instanceof List<?> answers)
                    || answers.size() != 1
                    || !(answers.get(0) instanceof Integer)) {
                hardFails.add("answer.invalid_fill_answer");
                repairHints.add("FILL questions must use a single index inside an array shape.");
            }
        }
    }

    private String compact(String value) {
        return value == null ? "" : value.replace(" ", "");
    }

    private String fingerprint(String option) {
        if (option == null) {
            return "";
        }
        return option.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private Integer answerIndex(StructuredQuestionSchema candidate) {
        if (candidate.answer() instanceof Integer index) {
            return index;
        }
        if (candidate.answer() instanceof List<?> answers
                && !answers.isEmpty()
                && answers.get(0) instanceof Integer index) {
            return index;
        }
        return null;
    }

    public record AnswerQualityResult(
            int answerClarityScore,
            int distractorQualityScore,
            List<String> hardFailReasons,
            List<String> softWarnings,
            List<String> repairHints
    ) {
    }
}
