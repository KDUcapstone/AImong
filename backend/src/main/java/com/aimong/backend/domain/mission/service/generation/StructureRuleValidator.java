package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StructureRuleValidator {

    private final KerisCurriculumRegistry kerisCurriculumRegistry;

    public StructureRuleValidator(KerisCurriculumRegistry kerisCurriculumRegistry) {
        this.kerisCurriculumRegistry = kerisCurriculumRegistry;
    }

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        if (candidate.contentTags() != null && (candidate.contentTags().isEmpty() || candidate.contentTags().size() > 3)) {
            hardFails.add("structure.invalid_content_tag_count");
        }
        if (candidate.type() == null) {
            hardFails.add("structure.missing_type");
        }
        if (candidate.effectiveDifficulty() == null) {
            hardFails.add("structure.missing_difficulty");
        }

        kerisCurriculumRegistry.findMissionRule(candidate.missionCode()).ifPresent(rule -> {
            if (!rule.preferredQuestionTypes().isEmpty() && !rule.preferredQuestionTypes().contains(candidate.type().name())) {
                hardFails.add("structure.type_not_allowed_for_mission");
            }
            if (!rule.preferredContentTags().isEmpty()
                    && candidate.contentTags() != null
                    && candidate.contentTags().stream().noneMatch(rule.preferredContentTags()::contains)) {
                warnings.add("structure.content_tags_weakly_aligned");
                repairHints.add("Choose content tags that better match the mission's preferred focus.");
            }
        });

        if (candidate.options() != null && candidate.options().stream().anyMatch(option -> option == null || option.isBlank())) {
            hardFails.add("structure.blank_option_detected");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 8) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }
}
