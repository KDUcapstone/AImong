package com.aimong.backend.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BootstrapResponse(
        boolean authenticated,
        String authType,
        ParentSummary parent,
        List<ChildSummary> children,
        ChildSummary child,
        Boolean homeAvailable,
        Instant serverTime,
        LocalDate serverDate,
        String minimumAppVersion,
        boolean forceUpdateRequired
) {
    public static BootstrapResponse guest(Instant serverTime, LocalDate serverDate) {
        return new BootstrapResponse(false, "NONE", null, null, null, null, serverTime, serverDate, "1.0.0", false);
    }

    public record ParentSummary(
            String parentId,
            long childrenCount,
            boolean hasFcmToken
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChildSummary(
            UUID childId,
            String nickname,
            String profileImageType,
            Integer totalXp,
            Instant lastActiveAt
    ) {
    }
}
