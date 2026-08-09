package com.aimong.backend.domain.auth.dto;

public record NotificationSettingsResponse(
        boolean privacyAlertEnabled,
        boolean studyReminderEnabled,
        boolean returnRewardEnabled,
        boolean questRewardEnabled,
        boolean marketingEnabled
) {
}
