package com.aimong.backend.domain.mission.service.generation;

import java.util.regex.Pattern;

final class QuestionPromptSanitizer {

    private static final Pattern LEADING_ACTIVITY_CLAUSE = Pattern.compile(
            "^(?:수업|활동|모둠|발표|토의)[^.!?。]*(?:떠올리며|생각하며|살피며|상상하며)\\s*"
    );
    private static final Pattern LEADING_ACTIVITY_INTRO = Pattern.compile(
            "^[^.!?。]*(?:떠올리며|생각하며|살피며|상상하며)[^.!?。]*(?:골라 보세요|고르세요|판단해 보세요|빈칸을 채워 보세요|답해 보세요)[.!?。]?\\s*"
    );

    private QuestionPromptSanitizer() {
    }

    static String sanitizeQuestion(String question) {
        if (question == null) {
            return "";
        }
        String normalized = question.trim().replaceAll("\\s+", " ");
        normalized = LEADING_ACTIVITY_INTRO.matcher(normalized).replaceFirst("").trim();
        return LEADING_ACTIVITY_CLAUSE.matcher(normalized).replaceFirst("").trim();
    }
}
