package com.aimong.backend.domain.mission.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_mission_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyMissionQuestion {

    // PK: (daily_mission_id, order_no)
    @EmbeddedId
    private DailyMissionQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("dailyMissionId")
    @JoinColumn(name = "daily_mission_id", nullable = false)
    private DailyMission dailyMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBank question;
}
