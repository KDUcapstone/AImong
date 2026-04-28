package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CurriculumFitValidator {

    private static final List<String> STEP1_FORBIDDEN = List.of(
            "출처", "날짜", "근거 비교", "편향", "공정", "딜레마", "체크리스트"
    );
    private static final List<String> STEP2_FORBIDDEN = List.of(
            "국가 ai 정책", "산업 영향", "산업 보고서", "철학", "법 조문", "정책 보고서"
    );
    private static final List<String> STEP3_FORBIDDEN = List.of(
            "공리주의", "의무론", "법 조문", "정책 보고서", "산업 보고서"
    );
    private static final List<String> DAILY_CONTEXT = List.of(
            "학교", "숙제", "발표", "사진", "목소리", "앱", "친구", "교실", "카메라", "번역", "검색"
    );
    private static final List<String> STEP3_VERIFICATION_KEYWORDS = List.of(
            "확인", "비교", "근거", "출처", "날짜", "편향", "공정", "검증"
    );
    private static final List<String> DIRECT_POLICY_THEMES = List.of(
            "국가 ai 정책", "산업 영향", "국가 전략", "법률", "법안", "정책 보고서", "산업 보고서"
    );
    private static final List<String> TEACHER_META = List.of(
            "교사용", "교사", "평가 기준", "수업 설계", "학습 목표 진술"
    );

    private final KerisCurriculumRegistry kerisCurriculumRegistry;

    public CurriculumFitValidator(KerisCurriculumRegistry kerisCurriculumRegistry) {
        this.kerisCurriculumRegistry = kerisCurriculumRegistry;
    }

    public CurriculumFitResult validate(StructuredQuestionSchema candidate) {
        List<String> curriculumFails = new ArrayList<>();
        List<String> stageFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        Optional<KerisCurriculumRegistry.KerisMissionRule> optionalRule =
                kerisCurriculumRegistry.findMissionRule(candidate.missionCode());
        if (optionalRule.isEmpty()) {
            curriculumFails.add("curriculum.unknown_mission_code");
            return new CurriculumFitResult(0, 0, curriculumFails, stageFails, warnings, repairHints);
        }

        KerisCurriculumRegistry.KerisMissionRule rule = optionalRule.get();
        String text = ValidationTextUtils.normalize(String.join("\n",
                candidate.question() == null ? "" : candidate.question(),
                candidate.explanation() == null ? "" : candidate.explanation()
        ));

        if (!rule.preferredContentTags().isEmpty()
                && candidate.contentTags() != null
                && candidate.contentTags().stream().noneMatch(rule.preferredContentTags()::contains)) {
            warnings.add("curriculum.unaligned_content_tags");
            repairHints.add("Align content tags with the mission's preferred focus.");
        }
        if (violatesDifficultyRange(rule.stage(), candidate.effectiveDifficulty(), candidate.difficulty())) {
            stageFails.add("curriculum.difficulty_out_of_stage_range");
        }
        if (containsAny(text, rule.bannedConcepts())) {
            stageFails.add("curriculum.mission_banned_concept");
            repairHints.add("Rewrite the question to stay inside the mission's allowed concept range.");
        }
        if (containsDirectPolicyTheme(text)) {
            curriculumFails.add("curriculum.policy_or_industry_theme");
        }
        if (containsTeacherFacingMeta(text)) {
            curriculumFails.add("curriculum.teacher_facing_meta_language");
        }

        switch (rule.stage()) {
            case 1 -> {
                if (containsAny(text, STEP1_FORBIDDEN)) {
                    stageFails.add("curriculum.step1_contains_step3_concepts");
                }
            }
            case 2 -> {
                if (containsAny(text, STEP2_FORBIDDEN)) {
                    stageFails.add("curriculum.step2_out_of_scope_theme");
                }
                if (!containsAny(text, DAILY_CONTEXT)) {
                    warnings.add("curriculum.step2_missing_daily_context");
                    repairHints.add("Use a classroom, homework, presentation, app, photo, or voice context.");
                }
            }
            case 3 -> {
                if (containsAny(text, STEP3_FORBIDDEN)) {
                    stageFails.add("curriculum.step3_too_abstract_or_legalistic");
                }
                if (!containsAny(text, STEP3_VERIFICATION_KEYWORDS)) {
                    warnings.add("curriculum.step3_weak_verification_signal");
                    repairHints.add("Make Step 3 items explicitly require comparison, verification, or judgment.");
                }
            }
            default -> stageFails.add("curriculum.unknown_stage");
        }

        int curriculumScore = curriculumFails.isEmpty()
                ? Math.max(0, 100 - stageFails.size() * 10 - warnings.size() * 5)
                : 0;
        int stageScore = stageFails.isEmpty()
                ? Math.max(0, 100 - warnings.size() * 5)
                : 0;

        return new CurriculumFitResult(
                curriculumScore,
                stageScore,
                List.copyOf(curriculumFails),
                List.copyOf(stageFails),
                List.copyOf(warnings),
                List.copyOf(repairHints)
        );
    }

    private boolean violatesDifficultyRange(int stage, DifficultyBand band, int difficulty) {
        return switch (stage) {
            case 1 -> difficulty < 1 || difficulty > 2 || band == null;
            case 2 -> difficulty < 2 || difficulty > 3 || band == null;
            case 3 -> difficulty < 3 || difficulty > 4 || band == null;
            default -> true;
        };
    }

    private boolean containsDirectPolicyTheme(String text) {
        return containsAny(text, DIRECT_POLICY_THEMES);
    }

    private boolean containsTeacherFacingMeta(String text) {
        return containsAny(text, TEACHER_META);
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream()
                .map(ValidationTextUtils::normalize)
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(text::contains);
    }

    public record CurriculumFitResult(
            int curriculumScore,
            int stageScore,
            List<String> curriculumHardFails,
            List<String> stageHardFails,
            List<String> warnings,
            List<String> repairHints
    ) {
    }
}
