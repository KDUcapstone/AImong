package com.aimong.backend.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "currency_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurrencyTransaction {

    @Id
    @Column(name = "transaction_id")
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reason", nullable = false, columnDefinition = "currency_transaction_reason_enum")
    private CurrencyTransactionReason reason;

    @Column(name = "ref_type")
    private String refType;

    @Column(name = "ref_id")
    private String refId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static CurrencyTransaction create(
            UUID childId,
            int amount,
            int balanceAfter,
            CurrencyTransactionReason reason,
            String refType,
            String refId
    ) {
        CurrencyTransaction transaction = new CurrencyTransaction();
        transaction.id = UUID.randomUUID();
        transaction.childId = childId;
        transaction.amount = amount;
        transaction.balanceAfter = balanceAfter;
        transaction.reason = reason;
        transaction.refType = refType;
        transaction.refId = refId;
        transaction.createdAt = Instant.now();
        return transaction;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
