package com.aimong.backend.infra.fcm;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ParentNotificationSettingsRepository;
import com.aimong.backend.domain.privacy.entity.PrivacyDetectedType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
public class FcmNotificationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DAILY_PARENT_LIMIT = 5;
    private static final int DAILY_PRIVACY_CHILD_LIMIT = 5;

    private final FcmService fcmService;
    private final FcmNotificationEventRepository fcmNotificationEventRepository;
    private final ParentNotificationSettingsRepository notificationSettingsRepository;

    @Transactional
    public void sendPrivacyAlert(ChildProfile childProfile, PrivacyDetectedType detectedType) {
        ParentAccount parentAccount = childProfile.getParentAccount();
        if (!StringUtils.hasText(parentAccount.getFcmToken())) {
            return;
        }
        if (!privacyAlertEnabled(parentAccount)) {
            return;
        }

        Instant todayStart = todayStart();
        flushQueuedPrivacyAlerts(parentAccount, todayStart);

        if (isDailyParentLimitReached(parentAccount.getParentId(), todayStart)
                || isDailyChildPrivacyLimitReached(parentAccount.getParentId(), childProfile.getId(), todayStart)) {
            fcmNotificationEventRepository.save(FcmNotificationEvent.queuedPrivacyAlert(
                    parentAccount.getParentId(),
                    childProfile.getId(),
                    detectedType.name()
            ));
            return;
        }

        sendAfterCommit(parentAccount.getFcmToken(), privacyPayload(childProfile, detectedType));
        fcmNotificationEventRepository.save(FcmNotificationEvent.sent(
                parentAccount.getParentId(),
                childProfile.getId(),
                FcmNotificationType.PRIVACY_ALERT,
                detectedType.name(),
                1
        ));
    }

    @Transactional
    public void sendLearningReminder(ChildProfile childProfile, int daysMissed) {
        ParentAccount parentAccount = childProfile.getParentAccount();
        if (!StringUtils.hasText(parentAccount.getFcmToken())) {
            return;
        }
        if (!studyReminderEnabled(parentAccount)) {
            return;
        }

        Instant todayStart = todayStart();
        Instant threeDaysAgo = LocalDate.now(KST).minusDays(3).atStartOfDay(KST).toInstant();
        if (isDailyParentLimitReached(parentAccount.getParentId(), todayStart)
                || fcmNotificationEventRepository.countByParentIdAndChildIdAndNotificationTypeAndStatusAndSentAtGreaterThanEqual(
                parentAccount.getParentId(),
                childProfile.getId(),
                FcmNotificationType.LEARNING_REMINDER,
                FcmNotificationStatus.SENT,
                threeDaysAgo
        ) > 0) {
            return;
        }

        sendAfterCommit(parentAccount.getFcmToken(), new FcmPayload(
                "학습 리마인더",
                "자녀가 " + daysMissed + "일째 학습을 쉬고 있어요. 한마디 해주세요!",
                Map.of(
                        "type", FcmNotificationType.LEARNING_REMINDER.name(),
                        "childId", childProfile.getId().toString(),
                        "daysMissed", String.valueOf(daysMissed)
                )
        ));
        fcmNotificationEventRepository.save(FcmNotificationEvent.sent(
                parentAccount.getParentId(),
                childProfile.getId(),
                FcmNotificationType.LEARNING_REMINDER,
                null,
                1
        ));
    }

    @Transactional
    public void sendQuestCompleteRequest(ChildProfile childProfile, UUID questId, String questTitle) {
        ParentAccount parentAccount = childProfile.getParentAccount();
        if (!StringUtils.hasText(parentAccount.getFcmToken())) {
            return;
        }
        if (!questRewardEnabled(parentAccount)) {
            return;
        }
        String refId = questId.toString();
        if (fcmNotificationEventRepository.existsByParentIdAndNotificationTypeAndRefId(
                parentAccount.getParentId(),
                FcmNotificationType.QUEST_COMPLETE_REQUEST,
                refId
        )) {
            return;
        }

        sendAfterCommit(parentAccount.getFcmToken(), new FcmPayload(
                "Custom quest completed",
                childProfile.getNickname() + " requested confirmation for " + questTitle,
                Map.of(
                        "type", FcmNotificationType.QUEST_COMPLETE_REQUEST.name(),
                        "childId", childProfile.getId().toString(),
                        "questId", refId
                )
        ));
        fcmNotificationEventRepository.save(FcmNotificationEvent.sentWithRef(
                parentAccount.getParentId(),
                childProfile.getId(),
                FcmNotificationType.QUEST_COMPLETE_REQUEST,
                refId
        ));
    }

    @Transactional
    public void flushQueuedPrivacyAlerts(ParentAccount parentAccount) {
        if (!StringUtils.hasText(parentAccount.getFcmToken())) {
            return;
        }
        if (!privacyAlertEnabled(parentAccount)) {
            return;
        }
        flushQueuedPrivacyAlerts(parentAccount, todayStart());
    }

    private void flushQueuedPrivacyAlerts(ParentAccount parentAccount, Instant todayStart) {
        if (isDailyParentLimitReached(parentAccount.getParentId(), todayStart)) {
            return;
        }
        if (!privacyAlertEnabled(parentAccount)) {
            return;
        }

        List<FcmNotificationEvent> queued = fcmNotificationEventRepository
                .findByParentIdAndNotificationTypeInAndStatusOrderByQueuedAtAsc(
                        parentAccount.getParentId(),
                        List.of(FcmNotificationType.PRIVACY_ALERT),
                        FcmNotificationStatus.QUEUED
                );
        if (queued.isEmpty()) {
            return;
        }

        int childCount = (int) queued.stream().map(FcmNotificationEvent::getChildId).distinct().count();
        sendAfterCommit(parentAccount.getFcmToken(), new FcmPayload(
                "개인정보 입력 감지",
                "오늘 자녀의 개인정보 입력 시도 " + queued.size() + "건이 감지됐어요.",
                Map.of(
                        "type", FcmNotificationType.PRIVACY_ALERT_BATCH.name(),
                        "queuedCount", String.valueOf(queued.size()),
                        "childCount", String.valueOf(childCount)
                )
        ));
        queued.forEach(FcmNotificationEvent::markSent);
        fcmNotificationEventRepository.save(FcmNotificationEvent.sent(
                parentAccount.getParentId(),
                null,
                FcmNotificationType.PRIVACY_ALERT_BATCH,
                null,
                queued.size()
        ));
    }

    private boolean isDailyParentLimitReached(String parentId, Instant todayStart) {
        return fcmNotificationEventRepository.countByParentIdAndStatusAndSentAtGreaterThanEqual(
                parentId,
                FcmNotificationStatus.SENT,
                todayStart
        ) >= DAILY_PARENT_LIMIT;
    }

    private boolean isDailyChildPrivacyLimitReached(String parentId, java.util.UUID childId, Instant todayStart) {
        return fcmNotificationEventRepository.countByParentIdAndChildIdAndNotificationTypeAndStatusAndSentAtGreaterThanEqual(
                parentId,
                childId,
                FcmNotificationType.PRIVACY_ALERT,
                FcmNotificationStatus.SENT,
                todayStart
        ) >= DAILY_PRIVACY_CHILD_LIMIT;
    }

    private boolean privacyAlertEnabled(ParentAccount parentAccount) {
        return notificationSettingsRepository.findById(parentAccount.getParentId())
                .map(settings -> settings.isPrivacyAlertEnabled())
                .orElse(true);
    }

    private boolean studyReminderEnabled(ParentAccount parentAccount) {
        return notificationSettingsRepository.findById(parentAccount.getParentId())
                .map(settings -> settings.isStudyReminderEnabled())
                .orElse(true);
    }

    private boolean questRewardEnabled(ParentAccount parentAccount) {
        return notificationSettingsRepository.findById(parentAccount.getParentId())
                .map(settings -> settings.isQuestRewardEnabled())
                .orElse(true);
    }

    private FcmPayload privacyPayload(ChildProfile childProfile, PrivacyDetectedType detectedType) {
        return new FcmPayload(
                "개인정보 입력 감지",
                "자녀가 AI에게 개인정보를 입력하려 했어요. 대화해보세요.",
                Map.of(
                        "type", FcmNotificationType.PRIVACY_ALERT.name(),
                        "childId", childProfile.getId().toString(),
                        "detectedType", detectedType.name()
                )
        );
    }

    private Instant todayStart() {
        return LocalDate.now(KST).atStartOfDay(KST).toInstant();
    }

    private void sendAfterCommit(String token, FcmPayload payload) {
        Runnable sender = () -> fcmService.sendToToken(token, payload);
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
}
