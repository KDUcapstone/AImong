package com.aimong.backend.domain.auth.service;

import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChildActivityService {

    private static final Duration TOUCH_THROTTLE = Duration.ofMinutes(5);

    private final ChildProfileRepository childProfileRepository;
    private final ConcurrentMap<UUID, Instant> recentTouches = new ConcurrentHashMap<>();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void touchLastActiveAt(UUID childId) {
        Instant now = Instant.now();
        Instant recentTouch = recentTouches.get(childId);
        if (recentTouch != null && recentTouch.isAfter(now.minus(TOUCH_THROTTLE))) {
            return;
        }
        int updated = childProfileRepository.touchLastActiveAtIfDue(childId, now, now.minus(TOUCH_THROTTLE));
        if (updated == 0 && !childProfileRepository.existsByIdAndDeletedAtIsNull(childId)) {
            recentTouches.remove(childId);
            throw new AimongException(ErrorCode.CHILD_NOT_FOUND);
        }
        recentTouches.put(childId, now);
    }
}
