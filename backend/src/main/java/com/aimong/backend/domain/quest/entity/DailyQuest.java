package com.aimong.backend.domain.quest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.DailyQuestType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "daily_quests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_quests_child_date_type", columnNames = {"child_id", "quest_date", "quest_type"})
    }
)
@Check(constraints = "((completed = false AND completed_at IS NULL) OR (completed = true AND completed_at IS NOT NULL)) AND (reward_claimed = false OR completed = true)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "quest_date", nullable = false)
    private LocalDate questDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_type", nullable = false, columnDefinition = "daily_quest_type_enum")
    private DailyQuestType questType;

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
