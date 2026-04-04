package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DailyMissionQuestionId implements Serializable {

    @Column(name = "daily_mission_id", columnDefinition = "uuid")
    private UUID dailyMissionId;

    @Column(name = "order_no")
    private Short orderNo;
}
