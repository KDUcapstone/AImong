package com.aimong.backend.infra.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    @Async
    public void sendToToken(String token, FcmPayload payload) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        FcmPayload displayPayload = normalizeDisplayPayload(payload);
        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(displayPayload.title())
                        .setBody(displayPayload.body())
                        .build());

        displayPayload.dataWithDisplayFields().forEach(messageBuilder::putData);

        try {
            FirebaseMessaging.getInstance().send(messageBuilder.build());
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM send failed: code={}, message={}",
                    exception.getMessagingErrorCode(),
                    exception.getMessage());
        } catch (RuntimeException exception) {
            log.warn("FCM send failed: message={}", exception.getMessage());
        }
    }

    private FcmPayload normalizeDisplayPayload(FcmPayload payload) {
        if (payload == null || payload.data() == null) {
            return payload;
        }
        String type = payload.data().get("type");
        if (type == null) {
            return payload;
        }
        return switch (type) {
            case "GACHA_LEVEL_UP" -> new FcmPayload(
                    "Gacha level up",
                    "Your child's Aimong collection is growing.",
                    payload.data()
            );
            case "LEARNING_REMINDER" -> new FcmPayload(
                    "Learning reminder",
                    "Your child has not studied for " + payload.data().getOrDefault("daysMissed", "several") + " days.",
                    payload.data()
            );
            case "PRIVACY_ALERT" -> new FcmPayload(
                    "Privacy alert",
                    "Your child may have entered personal information.",
                    payload.data()
            );
            case "PRIVACY_ALERT_BATCH" -> new FcmPayload(
                    "Privacy alert summary",
                    payload.data().getOrDefault("queuedCount", "Several") + " privacy-risk attempts were detected today.",
                    payload.data()
            );
            default -> payload;
        };
    }

    @Async
    public void sendGachaLevelUpToParent(String token, int gachaPullCount) {
        sendToToken(token, new FcmPayload(
                "레벨 업!",
                "자녀의 아이몽이 더 강해졌어요! 함께 축하해주세요",
                Map.of(
                        "type", "GACHA_LEVEL_UP",
                        "gachaPullCount", String.valueOf(gachaPullCount)
                )
        ));
    }
}
