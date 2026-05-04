package com.aimong.backend.infra.fcm;

import java.util.Map;

public record FcmPayload(
        String title,
        String body,
        Map<String, String> data
) {
}
