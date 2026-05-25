package com.aimong.backend.domain.stagereward.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stage_completion_rewards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageCompletionReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentAccount parentAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile childProfile;

    @Column(name = "stage_number", nullable = false)
    private int stageNumber;

    @Column(name = "reward_text", length = 100)
    private String rewardText;

    @Column(name = "is_triggered", nullable = false)
    private boolean triggered;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static StageCompletionReward create(
            ParentAccount parentAccount,
            ChildProfile childProfile,
            int stageNumber,
            String rewardText
    ) {
        StageCompletionReward reward = new StageCompletionReward();
        reward.parentAccount = parentAccount;
        reward.childProfile = childProfile;
        reward.stageNumber = stageNumber;
        reward.rewardText = normalizeBlank(rewardText);
        reward.triggered = false;
        return reward;
    }

    public void updateRewardText(String rewardText) {
        this.rewardText = normalizeBlank(rewardText);
        this.updatedAt = Instant.now();
    }

    public void markTriggered(Instant triggeredAt) {
        this.triggered = true;
        this.triggeredAt = triggeredAt;
        this.updatedAt = triggeredAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
