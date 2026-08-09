package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ParentNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentNotificationSettingsRepository extends JpaRepository<ParentNotificationSettings, String> {
}
