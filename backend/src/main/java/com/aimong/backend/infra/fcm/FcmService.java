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

        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(payload.title())
                        .setBody(payload.body())
                        .build());

        payload.data().forEach(messageBuilder::putData);

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
