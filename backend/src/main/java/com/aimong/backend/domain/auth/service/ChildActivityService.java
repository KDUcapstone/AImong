package com.aimong.backend.domain.auth.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChildActivityService {

    private static final Duration TOUCH_THROTTLE = Duration.ofMinutes(5);

    private final ChildProfileRepository childProfileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void touchLastActiveAt(UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (childProfile.getDeletedAt() != null) {
            throw new AimongException(ErrorCode.CHILD_NOT_FOUND);
        }
        Instant now = Instant.now();
        Instant lastActiveAt = childProfile.getLastActiveAt();
        if (lastActiveAt == null || lastActiveAt.isBefore(now.minus(TOUCH_THROTTLE))) {
            childProfile.touchLastActiveAt(now);
        }
    }
}
