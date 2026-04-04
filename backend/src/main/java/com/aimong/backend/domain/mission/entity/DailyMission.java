package com.aimong.backend.domain.mission.entity;

import com.aimong.backend.global.enums.DailyMissionStatus;
import com.aimong.backend.global.enums.QuestionSource;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_missions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mission_date", nullable = false, unique = true)
    private LocalDate missionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, columnDefinition = "question_source_enum")
    private QuestionSource generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "daily_mission_status_enum")
    @Builder.Default
    private DailyMissionStatus status = DailyMissionStatus.GENERATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public void archive() {
        this.status = DailyMissionStatus.ARCHIVED;
        this.archivedAt = OffsetDateTime.now();
    }
}
