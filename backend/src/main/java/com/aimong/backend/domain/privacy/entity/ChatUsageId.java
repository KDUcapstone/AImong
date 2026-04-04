package com.aimong.backend.domain.privacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatUsageId implements Serializable {

    @Column(name = "child_id", columnDefinition = "uuid")
    private UUID childId;

    @Column(name = "usage_date")
    private LocalDate usageDate;
}
