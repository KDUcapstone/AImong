package com.aimong.backend.domain.reward.repository;

import com.aimong.backend.domain.reward.entity.CurrencyTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyTransactionRepository extends JpaRepository<CurrencyTransaction, UUID> {
}
