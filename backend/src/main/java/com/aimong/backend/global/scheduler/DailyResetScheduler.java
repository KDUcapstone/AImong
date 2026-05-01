package com.aimong.backend.global.scheduler;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DailyResetScheduler {

    private final StreakRecordRepository streakRecordRepository;
    private final ChildProfileRepository childProfileRepository;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processStreakReset() {
        LocalDate today = KstDateUtils.today();
        streakRecordRepository.findStaleTodayMissionCounts(today)
                .forEach(StreakRecord::resetTodayMissionCount);

        LocalDate yesterday = today.minusDays(1);
        for (StreakRecord streakRecord : streakRecordRepository.findMissedActiveStreaksForReset(yesterday)) {
            ChildProfile profile = childProfileRepository.findWithLockById(streakRecord.getChildId())
                    .orElse(null);
            if (profile != null && profile.consumeShieldIfAvailable()) {
                streakRecord.resetTodayMissionCount();
            } else {
                streakRecord.resetStreak();
            }
        }
    }
}
