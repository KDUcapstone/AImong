package com.aimong.backend.domain.quest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.WeeklyQuestType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "weekly_quests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_weekly_quests_child_week_type", columnNames = {"child_id", "week_start", "quest_type"})
    }
)
@Check(constraints = "((completed = false AND completed_at IS NULL) OR (completed = true AND completed_at IS NOT NULL)) AND (reward_claimed = false OR completed = true)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WeeklyQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_type", nullable = false, columnDefinition = "weekly_quest_type_enum")
    private WeeklyQuestType questType;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "reward_claimed", nullable = false)
    @Builder.Default
    private Boolean rewardClaimed = false;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public void complete() {
        this.completed = true;
        this.completedAt = OffsetDateTime.now();
    }

    public void claimReward() {
        this.rewardClaimed = true;
    }
}
