package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class QuestionValidator {

    private static final Set<String> ALLOWED_CONTENT_TAGS = Set.of(
            "FACT", "PRIVACY", "PROMPT", "SAFETY", "VERIFICATION"
    );

    private final KerisCurriculumRegistry kerisCurriculumRegistry;
    private final ChildSafetyFilter childSafetyFilter;

    public QuestionValidator(
            KerisCurriculumRegistry kerisCurriculumRegistry,
            ChildSafetyFilter childSafetyFilter
    ) {
        this.kerisCurriculumRegistry = kerisCurriculumRegistry;
        this.childSafetyFilter = childSafetyFilter;
    }

    public List<String> validate(StructuredQuestionSchema candidate) {
        List<String> errors = new ArrayList<>();

        if (candidate.missionCode() == null || candidate.missionCode().isBlank()) {
            errors.add("missing missionCode");
        }
        if (candidate.packNo() < 1 || candidate.packNo() > 6) {
            errors.add("packNo must be 1..6");
        }
        if (candidate.difficultyBand() == null) {
            errors.add("missing difficultyBand");
        }
        if (candidate.type() == null) {
            errors.add("missing type");
        }
        if (candidate.question() == null || candidate.question().isBlank()) {
            errors.add("missing question");
        }
        if (candidate.explanation() == null || candidate.explanation().isBlank()) {
            errors.add("missing explanation");
        }
        if (candidate.difficulty() < 1 || candidate.difficulty() > 4) {
            errors.add("difficulty must be 1..4");
        }

        errors.addAll(validateMissionRule(candidate));
        errors.addAll(validateTypeShape(candidate));
        errors.addAll(validateContentTags(candidate));
        errors.addAll(validateExplanation(candidate));
        errors.addAll(childSafetyFilter.validate(candidate));

        return errors;
    }

    private List<String> validateMissionRule(StructuredQuestionSchema candidate) {
        return kerisCurriculumRegistry.findMissionRule(candidate.missionCode())
                .map(rule -> {
                    List<String> errors = new ArrayList<>();
                    if (!rule.preferredQuestionTypes().isEmpty() && !rule.preferredQuestionTypes().contains(candidate.type().name())) {
                        errors.add("question type not allowed by mission rule");
                    }
                    if (candidate.curriculumRef() == null || candidate.curriculumRef().isBlank()) {
                        errors.add("missing curriculumRef");
                    }
                    if (violatesDifficultyRange(rule.stage(), candidate.difficultyBand(), candidate.difficulty())) {
                        errors.add("difficulty out of stage range");
                    }
                    if (containsBannedConcept(rule.bannedConcepts(), candidate.question(), candidate.explanation())) {
                        errors.add("stage guardrail banned concept detected");
                    }
                    return errors;
                })
                .orElseGet(() -> List.of("unknown missionCode"));
    }

    private boolean violatesDifficultyRange(int stage, DifficultyBand band, int difficulty) {
        return switch (stage) {
            case 1 -> difficulty < 1 || difficulty > 2 || band == null;
            case 2 -> difficulty < 2 || difficulty > 3 || band == null;
            case 3 -> difficulty < 3 || difficulty > 4 || band == null;
            default -> true;
        };
    }

    private boolean containsBannedConcept(List<String> bannedConcepts, String question, String explanation) {
        String normalized = (question + " " + explanation).toLowerCase(Locale.ROOT);
        return bannedConcepts.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> value.length() >= 3)
                .anyMatch(normalized::contains);
    }

    private List<String> validateTypeShape(StructuredQuestionSchema candidate) {
        if (candidate.type() == null) {
            return List.of();
        }

        return switch (candidate.type()) {
            case OX -> validateOx(candidate);
            case MULTIPLE -> validateMultiple(candidate);
            case FILL -> validateFill(candidate);
            case SITUATION -> validateSituation(candidate);
        };
    }

    private List<String> validateOx(StructuredQuestionSchema candidate) {
        List<String> errors = new ArrayList<>();
        if (candidate.options() != null) {
            errors.add("OX options must be null");
        }
        if (!(candidate.answer() instanceof Boolean)) {
            errors.add("OX answer must be boolean");
        }
        return errors;
    }

    private List<String> validateMultiple(StructuredQuestionSchema candidate) {
        List<String> errors = new ArrayList<>();
        if (candidate.options() == null || candidate.options().size() != 4) {
            errors.add("MULTIPLE options must be 4");
        }
        if (!(candidate.answer() instanceof Integer answer) || answer < 0 || answer > 3) {
            errors.add("MULTIPLE answer must be index 0..3");
        }
        return errors;
    }

    private List<String> validateFill(StructuredQuestionSchema candidate) {
        List<String> errors = new ArrayList<>();
        if (candidate.options() == null || candidate.options().size() < 4 || candidate.options().size() > 5) {
            errors.add("FILL options must be 4..5");
        }
        if (!(candidate.answer() instanceof List<?> answers) || answers.size() != 1) {
            errors.add("FILL answer must be single index list");
        }
        return errors;
    }

    private List<String> validateSituation(StructuredQuestionSchema candidate) {
        List<String> errors = new ArrayList<>();
        if (candidate.options() == null || candidate.options().size() < 2 || candidate.options().size() > 4) {
            errors.add("SITUATION options must be 2..4");
        }
        if (!(candidate.answer() instanceof Integer answer) || candidate.options() == null
                || answer < 0 || answer >= candidate.options().size()) {
            errors.add("SITUATION answer index out of range");
        }
        return errors;
    }

    private List<String> validateContentTags(StructuredQuestionSchema candidate) {
        if (candidate.contentTags() == null || candidate.contentTags().isEmpty()) {
            return List.of("contentTags required");
        }

        List<String> errors = new ArrayList<>();
        for (String tag : candidate.contentTags()) {
            if (!ALLOWED_CONTENT_TAGS.contains(tag)) {
                errors.add("unsupported content tag: " + tag);
            }
        }
        return errors;
    }

    private List<String> validateExplanation(StructuredQuestionSchema candidate) {
        String explanation = candidate.explanation();
        if (explanation == null || explanation.isBlank()) {
            return List.of();
        }

        long sentenceCount = explanation.chars()
                .filter(ch -> ch == '.' || ch == '!' || ch == '?' || ch == '。')
                .count();
        if (sentenceCount == 0) {
            sentenceCount = 1;
        }
        return sentenceCount <= 2 ? List.of() : List.of("explanation must be 2 sentences or fewer");
    }
}
