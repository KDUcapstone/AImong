package com.aimong.backend.domain.auth.dto;

public record NotificationSettingsRequest(
        Boolean privacyAlertEnabled,
        Boolean studyReminderEnabled,
        Boolean returnRewardEnabled,
        Boolean questRewardEnabled,
        Boolean marketingEnabled
) {
    public boolean hasNoValues() {
        return privacyAlertEnabled == null
                && studyReminderEnabled == null
                && returnRewardEnabled == null
                && questRewardEnabled == null
                && marketingEnabled == null;
    }
}
