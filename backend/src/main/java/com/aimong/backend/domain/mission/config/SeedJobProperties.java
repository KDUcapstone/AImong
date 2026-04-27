package com.aimong.backend.domain.mission.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aimong.mission.seed")
public record SeedJobProperties(
        boolean enabled,
        boolean autoRun,
        boolean persist
) {
}
