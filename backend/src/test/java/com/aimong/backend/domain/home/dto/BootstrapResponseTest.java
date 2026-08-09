package com.aimong.backend.domain.home.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BootstrapResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void guestResponseOmitsNullRoutingFields() throws Exception {
        BootstrapResponse response = BootstrapResponse.guest(
                Instant.parse("2026-05-10T09:00:00Z"),
                LocalDate.parse("2026-05-10")
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertThat(json.has("authenticated")).isTrue();
        assertThat(json.has("authType")).isTrue();
        assertThat(json.has("serverTime")).isTrue();
        assertThat(json.has("serverDate")).isTrue();
        assertThat(json.has("minimumAppVersion")).isTrue();
        assertThat(json.has("forceUpdateRequired")).isTrue();
        assertThat(json.has("parent")).isFalse();
        assertThat(json.has("children")).isFalse();
        assertThat(json.has("child")).isFalse();
        assertThat(json.has("homeAvailable")).isFalse();
    }

    @Test
    void parentChildSummaryOmitsNullChildOnlyFields() throws Exception {
        BootstrapResponse response = new BootstrapResponse(
                true,
                "PARENT",
                new BootstrapResponse.ParentSummary("firebase_uid", 1, true),
                List.of(new BootstrapResponse.ChildSummary(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "민준",
                        "SPROUT",
                        null,
                        Instant.parse("2026-05-10T09:00:00Z")
                )),
                null,
                null,
                Instant.parse("2026-05-10T09:00:00Z"),
                LocalDate.parse("2026-05-10"),
                "1.0.0",
                false
        );

        JsonNode json = objectMapper.valueToTree(response);
        JsonNode childSummary = json.get("children").get(0);

        assertThat(json.has("child")).isFalse();
        assertThat(json.has("homeAvailable")).isFalse();
        assertThat(childSummary.has("childId")).isTrue();
        assertThat(childSummary.has("nickname")).isTrue();
        assertThat(childSummary.has("profileImageType")).isTrue();
        assertThat(childSummary.has("lastActiveAt")).isTrue();
        assertThat(childSummary.has("totalXp")).isFalse();
    }

    @Test
    void childResponseOmitsNullParentFieldsAndLastActiveAt() throws Exception {
        BootstrapResponse response = new BootstrapResponse(
                true,
                "CHILD",
                null,
                null,
                new BootstrapResponse.ChildSummary(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "민준",
                        "SPROUT",
                        150,
                        null
                ),
                true,
                Instant.parse("2026-05-10T09:00:00Z"),
                LocalDate.parse("2026-05-10"),
                "1.0.0",
                false
        );

        JsonNode json = objectMapper.valueToTree(response);
        JsonNode child = json.get("child");

        assertThat(json.has("parent")).isFalse();
        assertThat(json.has("children")).isFalse();
        assertThat(json.has("homeAvailable")).isTrue();
        assertThat(child.has("childId")).isTrue();
        assertThat(child.has("nickname")).isTrue();
        assertThat(child.has("profileImageType")).isTrue();
        assertThat(child.has("totalXp")).isTrue();
        assertThat(child.has("lastActiveAt")).isFalse();
    }
}
