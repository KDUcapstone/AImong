package com.aimong.backend.global.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.repository.ParentAccountRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.util.KstDateUtils;
import com.aimong.backend.infra.fcm.FcmNotificationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FcmReminderSchedulerTest {

    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ParentAccountRepository parentAccountRepository;
    @Mock private StreakRecordRepository streakRecordRepository;
    @Mock private FcmNotificationService fcmNotificationService;

    @Test
    void flushQueuedPrivacyAlertsDelegatesForEveryParent() {
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        parent.updateFcmToken("parent-fcm-token");
        when(parentAccountRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(parent));

        scheduler().flushQueuedPrivacyAlerts();

        verify(fcmNotificationService).flushQueuedPrivacyAlerts(parent);
    }

    @Test
    void sendMissedLearningRemindersSendsAfterThreeMissedDays() {
        ChildProfile child = childProfile();
        StreakRecord streakRecord = StreakRecord.create(child.getId());
        streakRecord.recordMissionCompletion(KstDateUtils.today().minusDays(3));
        when(childProfileRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(child));
        when(streakRecordRepository.findById(child.getId())).thenReturn(Optional.of(streakRecord));

        scheduler().sendMissedLearningReminders();

        verify(fcmNotificationService).sendLearningReminder(child, 3);
    }

    @Test
    void sendMissedLearningRemindersSkipsRecentLearners() {
        ChildProfile child = childProfile();
        StreakRecord streakRecord = StreakRecord.create(child.getId());
        streakRecord.recordMissionCompletion(KstDateUtils.today().minusDays(2));
        when(childProfileRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(child));
        when(streakRecordRepository.findById(child.getId())).thenReturn(Optional.of(streakRecord));

        scheduler().sendMissedLearningReminders();

        verify(fcmNotificationService, never()).sendLearningReminder(child, 2);
    }

    private FcmReminderScheduler scheduler() {
        return new FcmReminderScheduler(
                childProfileRepository,
                parentAccountRepository,
                streakRecordRepository,
                fcmNotificationService
        );
    }

    private ChildProfile childProfile() {
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        parent.updateFcmToken("parent-fcm-token");
        return ChildProfile.create(parent, "child", "123456");
    }
}
