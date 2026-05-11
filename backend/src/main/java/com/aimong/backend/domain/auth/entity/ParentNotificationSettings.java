package com.aimong.backend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "parent_notification_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParentNotificationSettings {

    @Id
    @Column(name = "parent_id", nullable = false)
    private String parentId;

    @Column(name = "privacy_alert_enabled", nullable = false)
    private boolean privacyAlertEnabled = true;

    @Column(name = "study_reminder_enabled", nullable = false)
    private boolean studyReminderEnabled = true;

    @Column(name = "return_reward_enabled", nullable = false)
    private boolean returnRewardEnabled = true;

    @Column(name = "quest_reward_enabled", nullable = false)
    private boolean questRewardEnabled = true;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ParentNotificationSettings createDefault(String parentId) {
        ParentNotificationSettings settings = new ParentNotificationSettings();
        settings.parentId = parentId;
        settings.updatedAt = Instant.now();
        return settings;
    }

    public void update(
            Boolean privacyAlertEnabled,
            Boolean studyReminderEnabled,
            Boolean returnRewardEnabled,
            Boolean questRewardEnabled,
            Boolean marketingEnabled
    ) {
        if (privacyAlertEnabled != null) {
            this.privacyAlertEnabled = privacyAlertEnabled;
        }
        if (studyReminderEnabled != null) {
            this.studyReminderEnabled = studyReminderEnabled;
        }
        if (returnRewardEnabled != null) {
            this.returnRewardEnabled = returnRewardEnabled;
        }
        if (questRewardEnabled != null) {
            this.questRewardEnabled = questRewardEnabled;
        }
        if (marketingEnabled != null) {
            this.marketingEnabled = marketingEnabled;
        }
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
