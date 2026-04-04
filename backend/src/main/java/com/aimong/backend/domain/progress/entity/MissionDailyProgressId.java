package com.aimong.backend.domain.progress.entity;

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
public class MissionDailyProgressId implements Serializable {

    @Column(name = "child_id", columnDefinition = "uuid")
    private UUID childId;

    @Column(name = "mission_id", columnDefinition = "uuid")
    private UUID missionId;

    @Column(name = "progress_date")
    private LocalDate progressDate;
}
