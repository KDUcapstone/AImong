package com.aimong.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String chatApiKey,
        String missionsApiKey,
        String baseUrl,
        String responsesPath,
        boolean mockEnabled
) {

    public OpenAiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (responsesPath == null || responsesPath.isBlank()) {
            responsesPath = "/responses";
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean isChatConfigured() {
        return resolvedChatApiKey() != null && !resolvedChatApiKey().isBlank();
    }

    public boolean isMissionsConfigured() {
        return resolvedMissionsApiKey() != null && !resolvedMissionsApiKey().isBlank();
    }

    public String resolvedChatApiKey() {
        return firstNonBlank(chatApiKey, apiKey);
    }

    public String resolvedMissionsApiKey() {
        return firstNonBlank(missionsApiKey, apiKey);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
