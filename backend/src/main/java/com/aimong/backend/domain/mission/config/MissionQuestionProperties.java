package com.aimong.backend.domain.mission.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aimong.mission.question")
public record MissionQuestionProperties(
        int setSize,
        int attemptTtlMinutes,
        boolean dynamicGenerationEnabled
) {

    public MissionQuestionProperties {
        if (setSize <= 0) {
            setSize = 10;
        }
        if (attemptTtlMinutes <= 0) {
            attemptTtlMinutes = 30;
        }
    }
}
