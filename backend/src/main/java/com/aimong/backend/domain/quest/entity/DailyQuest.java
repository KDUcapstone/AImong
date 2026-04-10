package com.aimong.backend.domain.quest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.global.enums.DailyQuestType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 데일리 퀘스트 현황
 * MISSION_1(미션 1개) / XP_20(오늘 XP 20) / CHAT_GPT(챗봇 대화) / ALL_3(3개 모두)
 * 매일 자정 스케줄러가 새 행 생성
 */
@Entity
@Table(name = "daily_quests")
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
