package com.aimong.backend.domain.mission.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aimong.mission.generation")
public record QuestionGenerationProperties(
        int targetPoolPerMission,
        int questionsPerPack,
        int softRefillTrigger,
        int hardRefillTrigger,
        int asyncGenerateBatch,
        long asyncRefillFixedDelayMs,
        int syncGenerateBatch,
        int miniMaxRetry
) {

    public QuestionGenerationProperties {
        if (targetPoolPerMission <= 0) {
            targetPoolPerMission = 60;
        }
        if (questionsPerPack <= 0) {
            questionsPerPack = 10;
        }
        if (softRefillTrigger <= 0) {
            softRefillTrigger = 36;
        }
        if (hardRefillTrigger <= 0) {
            hardRefillTrigger = 18;
        }
        if (asyncGenerateBatch <= 0) {
            asyncGenerateBatch = 10;
        }
        if (asyncRefillFixedDelayMs <= 0) {
            asyncRefillFixedDelayMs = 30_000L;
        }
        if (syncGenerateBatch <= 0) {
            syncGenerateBatch = 10;
        }
        if (miniMaxRetry <= 0) {
            miniMaxRetry = 2;
        }
    }
}
