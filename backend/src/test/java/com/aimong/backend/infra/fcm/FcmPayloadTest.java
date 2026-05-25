package com.aimong.backend.infra.fcm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FcmPayloadTest {

    @Test
    void dataWithDisplayFieldsKeepsTypeAndAddsTitleAndBody() {
        FcmPayload payload = new FcmPayload(
                "개인정보 입력 감지",
                "자녀가 AI에게 개인정보를 입력하려 했어요. 대화해보세요.",
                Map.of(
                        "type", FcmNotificationType.PRIVACY_ALERT.name(),
                        "childId", "child-id"
                )
        );

        Map<String, String> data = payload.dataWithDisplayFields();

        assertThat(data)
                .containsEntry("type", FcmNotificationType.PRIVACY_ALERT.name())
                .containsEntry("childId", "child-id")
                .containsEntry("title", "개인정보 입력 감지")
                .containsEntry("body", "자녀가 AI에게 개인정보를 입력하려 했어요. 대화해보세요.");
    }
}
