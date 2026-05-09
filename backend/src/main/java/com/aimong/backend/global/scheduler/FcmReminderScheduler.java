package com.aimong.backend.global.scheduler;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ParentAccountRepository;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.util.KstDateUtils;
import com.aimong.backend.infra.fcm.FcmNotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FcmReminderScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MISSED_LEARNING_REMINDER_DAYS = 3;

    private final ChildProfileRepository childProfileRepository;
    private final ParentAccountRepository parentAccountRepository;
    private final StreakRecordRepository streakRecordRepository;
    private final FcmNotificationService fcmNotificationService;

    @Scheduled(cron = "0 50 8 * * *", zone = "Asia/Seoul")
    @Transactional
    public void flushQueuedPrivacyAlerts() {
        parentAccountRepository.findAll()
                .forEach(fcmNotificationService::flushQueuedPrivacyAlerts);
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendMissedLearningReminders() {
        LocalDate today = KstDateUtils.today();
        for (ChildProfile childProfile : childProfileRepository.findAll()) {
            LocalDate baseDate = streakRecordRepository.findById(childProfile.getId())
                    .map(record -> record.getLastCompletedDate() != null
                            ? record.getLastCompletedDate()
                            : createdDate(childProfile))
                    .orElseGet(() -> createdDate(childProfile));
            if (baseDate == null) {
                continue;
            }

            int daysMissed = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(baseDate, today));
            if (daysMissed >= MISSED_LEARNING_REMINDER_DAYS) {
                fcmNotificationService.sendLearningReminder(childProfile, daysMissed);
            }
        }
    }

    private LocalDate createdDate(ChildProfile childProfile) {
        return childProfile.getCreatedAt() == null ? null : childProfile.getCreatedAt().atZone(KST).toLocalDate();
    }
}
