package com.aimong.backend.infra.fcm;

import java.util.LinkedHashMap;
import java.util.Map;

public record FcmPayload(
        String title,
        String body,
        Map<String, String> data
) {
    public Map<String, String> dataWithDisplayFields() {
        Map<String, String> enriched = new LinkedHashMap<>();
        if (data != null) {
            enriched.putAll(data);
        }
        if (title != null && !title.isBlank()) {
            enriched.put("title", title);
        }
        if (body != null && !body.isBlank()) {
            enriched.put("body", body);
        }
        return Map.copyOf(enriched);
    }
}
