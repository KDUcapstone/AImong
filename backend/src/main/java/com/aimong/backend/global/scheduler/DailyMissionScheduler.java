package com.aimong.backend.global.scheduler;

import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DailyMissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyMissionScheduler.class);
    private static final int EXPECTED_ACTIVE_SET_COUNT = 96;

    private final MissionSetRepository missionSetRepository;

    @Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void verifyNextDayMissionSetsReady() {
        long activeSetCount = missionSetRepository.countByActiveTrue();
        if (activeSetCount != EXPECTED_ACTIVE_SET_COUNT) {
            log.warn("Mission set readiness check failed: expected={}, actual={}",
                    EXPECTED_ACTIVE_SET_COUNT,
                    activeSetCount);
            return;
        }
        log.info("Mission set readiness check passed: activeSetCount={}", activeSetCount);
    }
}
