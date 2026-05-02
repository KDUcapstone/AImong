package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MissionDailyProgressId implements Serializable {

    @Column(name = "child_id", columnDefinition = "uuid", nullable = false)
    private UUID childId;

    @Column(name = "mission_id", columnDefinition = "uuid", nullable = false)
    private UUID missionId;

    @Column(name = "progress_date", nullable = false)
    private LocalDate progressDate;
}
