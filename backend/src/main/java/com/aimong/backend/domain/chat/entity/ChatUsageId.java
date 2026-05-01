package com.aimong.backend.domain.chat.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class ChatUsageId implements Serializable {

    private UUID childId;
    private LocalDate usageDate;

    public ChatUsageId() {
    }

    public ChatUsageId(UUID childId, LocalDate usageDate) {
        this.childId = childId;
        this.usageDate = usageDate;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ChatUsageId that)) {
            return false;
        }
        return Objects.equals(childId, that.childId)
                && Objects.equals(usageDate, that.usageDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childId, usageDate);
    }
}
