package com.aimong.backend.global.scheduler;

import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.util.KstDateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WeeklyResetScheduler {

    private final ChildProfileRepository childProfileRepository;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void resetWeeklyXp() {
        childProfileRepository.resetWeeklyXp(KstDateUtils.currentWeekStart());
    }
}
