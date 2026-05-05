package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AnswerQualityValidator {

    private static final Pattern EXPLANATION_OPTION_NUMBER = Pattern.compile("(?:정답은\\s*)?(\\d+)번");
    private static final Pattern CHAR_COUNT_PATTERN = Pattern.compile("(\\d+)자");

    public AnswerQualityResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        validateAnswerReference(candidate, hardFails, repairHints);

        if (candidate.type() == QuestionType.MULTIPLE
                || candidate.type() == QuestionType.SITUATION
                || candidate.type() == QuestionType.FILL) {
            validateOptions(candidate, hardFails, warnings, repairHints);
            validateAnswerExposure(candidate, hardFails, warnings, repairHints);
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
                validateExplanationNumberMatchesAnswer(candidate, answerIndex, hardFails, repairHints);
                validateCorrectOptionDoesNotContradictStem(candidate, answerIndex, hardFails, repairHints);
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
        validateDistractorPlausibility(candidate, answerIndex, hardFails, repairHints);

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

    private void validateDistractorPlausibility(
            StructuredQuestionSchema candidate,
            int answerIndex,
            List<String> hardFails,
            List<String> repairHints
    ) {
        long implausibleDistractors = 0;
        for (int index = 0; index < candidate.options().size(); index++) {
            if (index == answerIndex) {
                continue;
            }
            if (isImplausibleDistractor(candidate.options().get(index))) {
                implausibleDistractors++;
            }
        }
        if (implausibleDistractors >= 2) {
            hardFails.add("answer.implausible_distractors");
            repairHints.add("Use plausible misconceptions as distractors instead of cartoonish or impossible statements.");
        }
    }

    private void validateAnswerExposure(
            StructuredQuestionSchema candidate,
            List<String> hardFails,
            List<String> warnings,
            List<String> repairHints
    ) {
        Integer answerIndex = answerIndex(candidate);
        if (candidate.options() == null || answerIndex == null || answerIndex < 0 || answerIndex >= candidate.options().size()) {
            return;
        }

        String question = candidate.question();
        String correct = candidate.options().get(answerIndex);
        double stemOverlap = meaningfulOverlapRatio(question, correct);
        if (candidate.type() == QuestionType.FILL && stemOverlap >= 0.55d) {
            hardFails.add("answer.fill_answer_overexposed_in_stem");
            repairHints.add("Do not reveal most of the blank answer in the stem. Ask for one missing concept or condition.");
            return;
        }
        if (stemOverlap >= 0.70d) {
            warnings.add("answer.correct_option_too_easy_from_stem");
            repairHints.add("Reduce direct wording overlap between the stem and the correct option.");
        }

        if (candidate.type() == QuestionType.FILL && correct.length() > 24) {
            warnings.add("answer.fill_correct_option_too_long");
            repairHints.add("Keep FILL correct options short, usually one word or a short phrase.");
        }
        if (candidate.effectiveDifficulty() == com.aimong.backend.domain.mission.entity.DifficultyBand.HIGH
                && isObviousHighBandAnswerSet(candidate, correct)) {
            hardFails.add("answer.high_band_too_obvious");
            repairHints.add("HIGH difficulty questions need a less obvious correct option and stronger distractors.");
        }
    }

    private void validateExplanationNumberMatchesAnswer(
            StructuredQuestionSchema candidate,
            int answerIndex,
            List<String> hardFails,
            List<String> repairHints
    ) {
        Matcher matcher = EXPLANATION_OPTION_NUMBER.matcher(candidate.explanation());
        if (matcher.find()) {
            int mentionedNumber = Integer.parseInt(matcher.group(1));
            if (mentionedNumber != answerIndex + 1) {
                hardFails.add("answer.explanation_option_number_mismatch");
                repairHints.add("Make answer index and explanation option number refer to the same choice.");
            }
        }
    }

    private void validateCorrectOptionDoesNotContradictStem(
            StructuredQuestionSchema candidate,
            int answerIndex,
            List<String> hardFails,
            List<String> repairHints
    ) {
        List<Integer> stemCounts = extractCharCounts(candidate.question());
        List<Integer> optionCounts = extractCharCounts(candidate.options().get(answerIndex));
        if (stemCounts.size() == 1 && !optionCounts.isEmpty() && optionCounts.stream().noneMatch(stemCounts.getFirst()::equals)) {
            hardFails.add("answer.correct_option_contradicts_stem");
            repairHints.add("Do not mark an option correct if it contradicts a concrete count or length in the stem.");
        }
    }

    private List<Integer> extractCharCounts(String value) {
        List<Integer> counts = new ArrayList<>();
        Matcher matcher = CHAR_COUNT_PATTERN.matcher(value == null ? "" : value);
        while (matcher.find()) {
            counts.add(Integer.parseInt(matcher.group(1)));
        }
        return counts;
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

    private double meaningfulOverlapRatio(String left, String right) {
        Set<String> leftTokens = meaningfulTokens(left);
        Set<String> rightTokens = meaningfulTokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(rightTokens);
        intersection.retainAll(leftTokens);
        return (double) intersection.size() / rightTokens.size();
    }

    private Set<String> meaningfulTokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : ValidationTextUtils.normalize(value).split("\\s+")) {
            String compacted = compact(token);
            if (compacted.length() < 2 || FILLER_TOKENS.contains(compacted)) {
                continue;
            }
            tokens.add(compacted);
        }
        return tokens;
    }

    private boolean isObviousHighBandAnswerSet(StructuredQuestionSchema candidate, String correct) {
        String normalizedCorrect = compact(ValidationTextUtils.normalize(correct));
        boolean correctTooEasy = normalizedCorrect.contains("짧고쉬운말")
                || normalizedCorrect.contains("구체적으로")
                || normalizedCorrect.contains("쉽게")
                || normalizedCorrect.contains("3문장")
                || normalizedCorrect.contains("한문장");
        if (!correctTooEasy || candidate.options() == null) {
            return false;
        }
        long obviouslyBadDistractors = candidate.options().stream()
                .filter(option -> !option.equals(correct))
                .map(ValidationTextUtils::normalize)
                .map(this::compact)
                .filter(option -> option.contains("전문용어")
                        || option.contains("아무조건")
                        || option.contains("무작위")
                        || option.contains("막적어")
                        || option.contains("길게")
                        || option.contains("자세히"))
                .count();
        return obviouslyBadDistractors >= 2;
    }

    private boolean isImplausibleDistractor(String option) {
        String normalized = compact(ValidationTextUtils.normalize(option));
        return normalized.contains("일부러틀")
                || normalized.contains("일부러작동")
                || normalized.contains("전기를아껴")
                || normalized.contains("사용자의기분")
                || normalized.contains("기분을읽")
                || normalized.contains("마음을읽")
                || normalized.contains("사람처럼일부러")
                || normalized.contains("알아서마법")
                || normalized.contains("무조건정답");
    }

    private static final Set<String> FILLER_TOKENS = Set.of(
            "것", "수", "때", "더", "잘", "가장", "어떤", "무엇", "해요", "합니다", "주세요",
            "으로", "로", "에게", "에서", "에는", "은", "는", "이", "가", "을", "를", "과", "와"
    );

    public record AnswerQualityResult(
            int answerClarityScore,
            int distractorQualityScore,
            List<String> hardFailReasons,
            List<String> softWarnings,
            List<String> repairHints
    ) {
    }
}
