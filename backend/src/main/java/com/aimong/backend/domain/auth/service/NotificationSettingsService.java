package com.aimong.backend.domain.auth.service;

import com.aimong.backend.domain.auth.dto.NotificationSettingsRequest;
import com.aimong.backend.domain.auth.dto.NotificationSettingsResponse;
import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentNotificationSettings;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.repository.ParentNotificationSettingsRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final ParentNotificationSettingsRepository settingsRepository;
    private final ChildProfileRepository childProfileRepository;

    @Transactional
    public NotificationSettingsResponse getSettings(String principalName) {
        ParentNotificationSettings settings = settingsForPrincipal(principalName);
        return toResponse(settings);
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(String principalName, NotificationSettingsRequest request) {
        if (request == null || request.hasNoValues()) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "수정할 값을 입력해주세요");
        }
        if (isChildPrincipal(principalName)) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        ParentNotificationSettings settings = settingsForPrincipal(principalName);
        settings.update(
                request.privacyAlertEnabled(),
                request.studyReminderEnabled(),
                request.returnRewardEnabled(),
                request.questRewardEnabled(),
                request.marketingEnabled()
        );
        return toResponse(settings);
    }

    private ParentNotificationSettings settingsForPrincipal(String principalName) {
        String parentId = resolveParentId(principalName);
        return settingsRepository.findById(parentId)
                .orElseGet(() -> settingsRepository.save(ParentNotificationSettings.createDefault(parentId)));
    }

    private String resolveParentId(String principalName) {
        try {
            UUID childId = UUID.fromString(principalName);
            ChildProfile childProfile = childProfileRepository.findByIdAndDeletedAtIsNull(childId)
                    .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
            return childProfile.getParentAccount().getParentId();
        } catch (IllegalArgumentException ignored) {
            return principalName;
        }
    }

    private boolean isChildPrincipal(String principalName) {
        try {
            UUID.fromString(principalName);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private NotificationSettingsResponse toResponse(ParentNotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.isPrivacyAlertEnabled(),
                settings.isStudyReminderEnabled(),
                settings.isReturnRewardEnabled(),
                settings.isQuestRewardEnabled(),
                settings.isMarketingEnabled()
        );
    }
}
