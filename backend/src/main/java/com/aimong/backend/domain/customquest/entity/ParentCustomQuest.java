package com.aimong.backend.domain.customquest.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "parent_custom_quests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ParentCustomQuest {

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

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "reward_text", nullable = false, length = 100)
    private String rewardText;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "custom_quest_status_enum")
    private CustomQuestStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ParentCustomQuest create(
            ParentAccount parentAccount,
            ChildProfile childProfile,
            String title,
            String description,
            String rewardText,
            Instant expiresAt
    ) {
        return new ParentCustomQuest(
                null,
                parentAccount,
                childProfile,
                title,
                normalizeBlank(description),
                rewardText,
                expiresAt,
                CustomQuestStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }

    public void requestCompletion(Instant completedAt) {
        this.status = CustomQuestStatus.PENDING_CONFIRM;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public void confirm(Instant confirmedAt) {
        this.status = CustomQuestStatus.COMPLETED;
        this.confirmedAt = confirmedAt;
        this.updatedAt = confirmedAt;
    }

    public void cancel(Instant cancelledAt) {
        this.status = CustomQuestStatus.CANCELLED;
        this.updatedAt = cancelledAt;
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
        return value == null || value.isBlank() ? null : value;
    }
}
