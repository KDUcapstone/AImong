package com.aimong.backend.domain.mission.entity;

import java.util.Locale;

public enum QuestionSourceType {
    STATIC,
    GPT;

    public static QuestionSourceType from(String value) {
        if (value == null || value.isBlank()) {
            return STATIC;
        }
        return QuestionSourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
