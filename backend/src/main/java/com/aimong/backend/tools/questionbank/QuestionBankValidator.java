package com.aimong.backend.tools.questionbank;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuestionBankValidator {

    public List<String> validate(QuestionBankDraft draft) {
        List<String> errors = new java.util.ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> prompts = new HashSet<>();

        for (QuestionDraft question : draft.questions()) {
            if (!ids.add(question.externalId())) {
                errors.add("duplicate externalId: " + question.externalId());
            }
            if (!prompts.add(question.missionCode() + "|" + question.question())) {
                errors.add("duplicate prompt in mission: " + question.externalId());
            }
            if (question.question() == null || question.question().isBlank()) {
                errors.add("blank question: " + question.externalId());
            }
            if (question.explanation() == null || question.explanation().isBlank()) {
                errors.add("blank explanation: " + question.externalId());
            }
            if (question.contentTags() == null || question.contentTags().isEmpty()) {
                errors.add("missing content tags: " + question.externalId());
            }

            switch (question.type()) {
                case "OX" -> validateOx(question, errors);
                case "MULTIPLE" -> validateMultiple(question, errors);
                case "FILL" -> validateFill(question, errors);
                case "SITUATION" -> validateSituation(question, errors);
                default -> errors.add("unknown type: " + question.externalId() + " -> " + question.type());
            }
        }

        if (draft.totalQuestionCount() != draft.questions().size()) {
            errors.add("totalQuestionCount mismatch");
        }
        return errors;
    }

    private void validateOx(QuestionDraft question, List<String> errors) {
        if (!(question.answer() instanceof Boolean)) {
            errors.add("OX answer must be boolean: " + question.externalId());
        }
        if (question.options() != null) {
            errors.add("OX options must be null: " + question.externalId());
        }
    }

    private void validateMultiple(QuestionDraft question, List<String> errors) {
        if (question.options() == null || question.options().size() != 4) {
            errors.add("MULTIPLE must have 4 options: " + question.externalId());
        }
        if (!(question.answer() instanceof Integer answer) || answer < 0 || answer > 3) {
            errors.add("MULTIPLE answer must be index 0..3: " + question.externalId());
        }
    }

    private void validateFill(QuestionDraft question, List<String> errors) {
        if (question.options() == null || question.options().size() < 4 || question.options().size() > 5) {
            errors.add("FILL must have 4..5 options: " + question.externalId());
        }
        if (!(question.answer() instanceof List<?> answers) || answers.isEmpty()) {
            errors.add("FILL answer must be a non-empty index list: " + question.externalId());
        }
    }

    private void validateSituation(QuestionDraft question, List<String> errors) {
        if (question.options() == null || question.options().size() < 2 || question.options().size() > 4) {
            errors.add("SITUATION must have 2..4 options: " + question.externalId());
        }
        if (!(question.answer() instanceof Integer answer) || answer < 0 || answer >= question.options().size()) {
            errors.add("SITUATION answer index out of range: " + question.externalId());
        }
    }
}
