package com.aimong.backend.domain.reward.entity;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "return_reward_claims",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_return_reward_claims_child_base_date",
            columnNames = {"child_id", "base_last_completed_date"}
        )
    }
)
@Check(constraints = "ticket_count BETWEEN 1 AND 3")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReturnRewardClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "base_last_completed_date", nullable = false)
    private LocalDate baseLastCompletedDate;

    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private OffsetDateTime claimedAt;

    @PrePersist
    protected void onCreate() {
        if (claimedAt == null) {
            claimedAt = OffsetDateTime.now();
        }
    }
}
