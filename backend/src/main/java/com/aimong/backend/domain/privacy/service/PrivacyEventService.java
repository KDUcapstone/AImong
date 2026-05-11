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
import com.aimong.backend.infra.fcm.FcmNotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivacyEventService {

    private final PrivacyEventRepository privacyEventRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final FcmNotificationService fcmNotificationService;

    @Transactional
    public PrivacyEventResponse record(UUID childId, PrivacyDetectedType detectedType, boolean masked) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        childActivityService.touchLastActiveAt(childId);

        privacyEventRepository.save(PrivacyEvent.create(childId, detectedType, masked));
        fcmNotificationService.sendPrivacyAlert(childProfile, detectedType);

        return new PrivacyEventResponse(true);
    }
}
