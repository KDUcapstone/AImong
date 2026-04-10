package com.aimong.backend.domain.quest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.WeeklyQuestType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 위클리 퀘스트 현황
 * XP_100(주간 XP 100) / MISSION_5(미션 5개) / CHAT_3(챗봇 3회)
 * week_start = 해당 주 월요일 날짜
 */
@Entity
@Table(name = "weekly_quests")
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
