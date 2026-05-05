package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import com.aimong.backend.domain.mission.entity.QuestionType;
import org.springframework.stereotype.Component;

@Component
public class NaturalnessValidator {

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        String question = candidate.question() == null ? "" : candidate.question().trim();
        String explanation = candidate.explanation() == null ? "" : candidate.explanation().trim();

        if (!question.isBlank()
                && !question.endsWith("?")
                && !question.endsWith("!")
                && !question.endsWith(".")) {
            warnings.add("naturalness.unfinished_stem_ending");
            repairHints.add("Make the stem end like a natural student-facing question.");
        }
        if (question.contains("  ") || explanation.contains("  ")) {
            warnings.add("naturalness.irregular_spacing");
        }
        if (candidate.type() == QuestionType.FILL) {
            validateFillStem(question, hardFails, warnings, repairHints);
        }
        if (candidate.type() == QuestionType.OX) {
            validateOxStem(candidate, question, hardFails, warnings, repairHints);
        }
        if (hasRepeatedPhrase(question)) {
            warnings.add("naturalness.repetitive_stem");
            repairHints.add("Rewrite the stem to avoid repetitive phrasing.");
        }
        if (candidate.options() != null && candidate.options().stream().allMatch(option -> option.startsWith("AI"))) {
            warnings.add("naturalness.options_too_patterned");
            repairHints.add("Vary option openings so they read like natural alternatives.");
        }
        if (candidate.options() != null) {
            int min = candidate.options().stream().mapToInt(String::length).min().orElse(0);
            int max = candidate.options().stream().mapToInt(String::length).max().orElse(0);
            if (min > 0 && max >= min * 3) {
                warnings.add("naturalness.option_tone_imbalance");
            }
        }
        if (!explanation.isBlank() && ValidationTextUtils.tokenJaccard(question, explanation) > 0.8d) {
            warnings.add("naturalness.explanation_too_close_to_stem");
            repairHints.add("Make the explanation sound like a reason, not a restatement.");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 12) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private void validateFillStem(
            String question,
            List<String> hardFails,
            List<String> warnings,
            List<String> repairHints
    ) {
        String compacted = question.replace(" ", "");
        if (compacted.contains("'____") || compacted.contains("\"____") || compacted.contains("'________") || compacted.contains("\"________")) {
            hardFails.add("naturalness.fill_blank_quoted_or_meta");
            repairHints.add("Do not quote the blank or describe it as an output slot. Write it as a natural sentence blank.");
        }
        if (question.contains("____ 있어요")
                || question.contains("____ 있습니다")
                || question.contains("____ 있다")) {
            hardFails.add("naturalness.fill_ungrammatical_blank_ending");
            repairHints.add("Make sure every FILL option creates a natural Korean sentence when inserted into the blank.");
        }
        if (question.contains("예:") || question.contains("예를 들어")) {
            hardFails.add("naturalness.fill_meta_example_stem");
            repairHints.add("Do not make the stem a meta example. Ask a direct classroom question with one blank.");
        }
        if (question.contains("질문에 넣을 조건은") || question.contains("출력 형식")) {
            warnings.add("naturalness.fill_stem_too_meta");
            repairHints.add("Make FILL stems sound like a quiz question, not a prompt template instruction.");
        }
    }

    private void validateOxStem(
            StructuredQuestionSchema candidate,
            String question,
            List<String> hardFails,
            List<String> warnings,
            List<String> repairHints
    ) {
        if (Boolean.TRUE.equals(candidate.answer())
                && isTautologicalPromptCondition(question)) {
            hardFails.add("naturalness.ox_tautological_condition_result");
            repairHints.add("OX questions should require judgment, not simply restate that AI follows the requested format.");
        }
        if (Boolean.TRUE.equals(candidate.answer())
                && question.contains("보통")
                && (question.contains("만들어 줘요") || question.contains("해 줘요"))) {
            warnings.add("naturalness.ox_vague_obvious_true_statement");
            repairHints.add("Avoid vague obvious true OX statements. Add a concrete comparison or misconception.");
        }
    }

    private boolean isTautologicalPromptCondition(String question) {
        boolean hasPromptCondition = question.contains("라고 하면")
                || question.contains("달라고 하면")
                || question.contains("라고 쓰면")
                || question.contains("조건을 붙이면");
        if (!hasPromptCondition) {
            return false;
        }
        boolean repeatedCount = question.matches(".*\\d+개[^.?!]*\\d+개.*")
                || question.matches(".*[한두세네다섯]\\s*줄[^.?!]*[한두세네다섯]\\s*줄.*")
                || question.matches(".*[한두세네다섯]\\s*문장[^.?!]*[한두세네다섯]\\s*문장.*");
        boolean directComplianceVerb = question.contains("만들어 줘요")
                || question.contains("써 줘요")
                || question.contains("적어 줘요")
                || question.contains("짧아질")
                || question.contains("간단해질");
        return repeatedCount && directComplianceVerb;
    }

    private boolean hasRepeatedPhrase(String text) {
        String normalized = ValidationTextUtils.normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        String[] tokens = normalized.split("\\s+");
        for (int index = 0; index < tokens.length - 2; index++) {
            String phrase = tokens[index] + " " + tokens[index + 1];
            int count = 0;
            for (int cursor = 0; cursor < tokens.length - 1; cursor++) {
                String other = tokens[cursor] + " " + tokens[cursor + 1];
                if (phrase.equals(other)) {
                    count++;
                }
            }
            if (count >= 3) {
                return true;
            }
        }
        return false;
    }
}
