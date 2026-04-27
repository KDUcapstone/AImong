package com.aimong.backend.domain.quest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.DailyQuestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "daily_quest_progress",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_quest_progress_child_type_date", columnNames = {"child_id", "quest_type", "date"})
    }
)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_type", nullable = false, columnDefinition = "daily_quest_type_enum")
    private DailyQuestType questType;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Integer currentValue = 0;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "reward_claimed", nullable = false)
    @Builder.Default
    private Boolean rewardClaimed = false;

    public void updateCurrentValue(int currentValue) {
        this.currentValue = currentValue;
    }

    public void complete() {
        this.completed = true;
    }

    public void claimReward() {
        this.rewardClaimed = true;
    }
}
