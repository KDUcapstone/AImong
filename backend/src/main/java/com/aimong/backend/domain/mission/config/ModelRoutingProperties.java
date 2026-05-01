package com.aimong.backend.domain.mission.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aimong.mission.model-routing")
public record ModelRoutingProperties(
        String defaultModel,
        String escalatedModel
) {

    public ModelRoutingProperties {
        if (defaultModel == null || defaultModel.isBlank()) {
            defaultModel = "gpt-5-mini";
        }
        if (escalatedModel == null || escalatedModel.isBlank()) {
            escalatedModel = "gpt-5.4-mini";
        }
    }
}
