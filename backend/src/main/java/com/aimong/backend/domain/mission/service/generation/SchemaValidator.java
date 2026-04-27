package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SchemaValidator {

    private static final Set<String> ALLOWED_CONTENT_TAGS = Set.of(
            "FACT", "PRIVACY", "PROMPT", "SAFETY", "VERIFICATION"
    );

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        if (candidate.missionCode() == null || candidate.missionCode().isBlank()) {
            hardFails.add("schema.missing_mission_code");
        }
        if (candidate.type() == null) {
            hardFails.add("schema.missing_type");
        }
        if (candidate.question() == null || candidate.question().isBlank()) {
            hardFails.add("schema.missing_question");
        }
        if (candidate.explanation() == null || candidate.explanation().isBlank()) {
            hardFails.add("schema.missing_explanation");
        }
        if (candidate.contentTags() == null || candidate.contentTags().isEmpty()) {
            hardFails.add("schema.missing_content_tags");
        }
        if (candidate.curriculumRef() == null || candidate.curriculumRef().isBlank()) {
            hardFails.add("schema.missing_curriculum_ref");
        }
        if (candidate.packNo() < 1 || candidate.packNo() > 6) {
            hardFails.add("schema.invalid_pack_no");
        }
        if (candidate.difficultyBand() == null) {
            hardFails.add("schema.missing_difficulty_band");
        }
        if (candidate.difficulty() < 1 || candidate.difficulty() > 4) {
            hardFails.add("schema.invalid_difficulty");
        }

        if (candidate.contentTags() != null) {
            for (String tag : candidate.contentTags()) {
                if (!ALLOWED_CONTENT_TAGS.contains(tag)) {
                    hardFails.add("schema.unsupported_content_tag");
                    repairHints.add("Use only official content tags.");
                    break;
                }
            }
        }

        if (candidate.type() != null) {
            hardFails.addAll(validateByType(candidate));
        }

        if (candidate.question() != null && candidate.question().trim().length() < 8) {
            warnings.add("schema.question_too_short");
            repairHints.add("Make the question stem more specific.");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 5) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private List<String> validateByType(StructuredQuestionSchema candidate) {
        return switch (candidate.type()) {
            case OX -> validateOx(candidate);
            case MULTIPLE -> validateMultiple(candidate);
            case FILL -> validateFill(candidate);
            case SITUATION -> validateSituation(candidate);
        };
    }

    private List<String> validateOx(StructuredQuestionSchema candidate) {
        List<String> failures = new ArrayList<>();
        if (candidate.options() != null) {
            failures.add("schema.ox_options_must_be_null");
        }
        if (!(candidate.answer() instanceof Boolean)) {
            failures.add("schema.ox_answer_must_be_boolean");
        }
        return failures;
    }

    private List<String> validateMultiple(StructuredQuestionSchema candidate) {
        List<String> failures = new ArrayList<>();
        if (candidate.options() == null || candidate.options().size() != 4) {
            failures.add("schema.multiple_options_count");
        }
        if (!(candidate.answer() instanceof Integer answer) || answer < 0 || answer > 3) {
            failures.add("schema.multiple_answer_range");
        }
        return failures;
    }

    private List<String> validateFill(StructuredQuestionSchema candidate) {
        List<String> failures = new ArrayList<>();
        if (candidate.options() == null || candidate.options().size() < 4 || candidate.options().size() > 5) {
            failures.add("schema.fill_options_count");
        }
        if (!(candidate.answer() instanceof List<?> answers) || answers.size() != 1 || !(answers.get(0) instanceof Integer)) {
            failures.add("schema.fill_answer_shape");
        }
        return failures;
    }

    private List<String> validateSituation(StructuredQuestionSchema candidate) {
        List<String> failures = new ArrayList<>();
        if (candidate.options() == null || candidate.options().size() < 2 || candidate.options().size() > 4) {
            failures.add("schema.situation_options_count");
        }
        if (!(candidate.answer() instanceof Integer answer)
                || candidate.options() == null
                || answer < 0
                || answer >= candidate.options().size()) {
            failures.add("schema.situation_answer_range");
        }
        return failures;
    }
}
