package com.aimong.backend.domain.privacy.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.privacy.dto.PrivacyEventResponse;
import com.aimong.backend.domain.privacy.entity.PrivacyDetectedType;
import com.aimong.backend.domain.privacy.entity.PrivacyEvent;
import com.aimong.backend.domain.privacy.repository.PrivacyEventRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.infra.fcm.FcmPayload;
import com.aimong.backend.infra.fcm.FcmService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PrivacyEventService {

    private static final int DAILY_PRIVACY_ALERT_LIMIT = 5;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PrivacyEventRepository privacyEventRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final FcmService fcmService;

    @Transactional
    public PrivacyEventResponse record(UUID childId, PrivacyDetectedType detectedType, boolean masked) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        childActivityService.touchLastActiveAt(childId);

        privacyEventRepository.save(PrivacyEvent.create(childId, detectedType, masked));
        sendPrivacyAlertAfterCommit(childProfile, detectedType);

        return new PrivacyEventResponse(true);
    }

    private void sendPrivacyAlertAfterCommit(ChildProfile childProfile, PrivacyDetectedType detectedType) {
        if (!shouldSendPrivacyAlert(childProfile)) {
            return;
        }

        String parentFcmToken = childProfile.getParentAccount().getFcmToken();
        if (!StringUtils.hasText(parentFcmToken)) {
            return;
        }

        Runnable sender = () -> fcmService.sendToToken(parentFcmToken, new FcmPayload(
                "Privacy alert",
                "Your child may have entered personal information while using AI.",
                Map.of(
                        "type", "PRIVACY_ALERT",
                        "childId", childProfile.getId().toString(),
                        "detectedType", detectedType.name()
                )
        ));

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sender.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sender.run();
            }
        });
    }

    private boolean shouldSendPrivacyAlert(ChildProfile childProfile) {
        Instant todayStart = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        long childAlertCount = privacyEventRepository.countByChildIdAndDetectedAtGreaterThanEqual(
                childProfile.getId(),
                todayStart
        );
        if (childAlertCount > DAILY_PRIVACY_ALERT_LIMIT) {
            return false;
        }

        var childIds = childProfileRepository.findAllByParentAccountParentIdOrderByCreatedAtAsc(
                        childProfile.getParentAccount().getParentId())
                .stream()
                .map(ChildProfile::getId)
                .toList();
        long parentAlertCount = privacyEventRepository.countByChildIdInAndDetectedAtGreaterThanEqual(
                childIds,
                todayStart
        );
        return parentAlertCount <= DAILY_PRIVACY_ALERT_LIMIT;
    }
}
