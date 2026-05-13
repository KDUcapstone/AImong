package com.aimong.backend.domain.reward.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.reward.entity.CurrencyTransaction;
import com.aimong.backend.domain.reward.entity.CurrencyTransactionReason;
import com.aimong.backend.domain.reward.repository.CurrencyTransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    public static final int HEART_REVIVE_COST = 10;
    public static final int STREAK_SHIELD_COST = 30;
    public static final int MISSION_CLEAR_GEAR = 30;

    private final CurrencyTransactionRepository currencyTransactionRepository;

    public void grantGear(
            ChildProfile childProfile,
            int amount,
            CurrencyTransactionReason reason,
            String refType,
            String refId
    ) {
        childProfile.addGear(amount);
        record(childProfile.getId(), amount, childProfile.getGear(), reason, refType, refId);
    }

    public boolean consumeGear(
            ChildProfile childProfile,
            int amount,
            CurrencyTransactionReason reason,
            String refType,
            String refId
    ) {
        if (!childProfile.consumeGear(amount)) {
            return false;
        }
        record(childProfile.getId(), -amount, childProfile.getGear(), reason, refType, refId);
        return true;
    }

    private void record(
            UUID childId,
            int amount,
            int balanceAfter,
            CurrencyTransactionReason reason,
            String refType,
            String refId
    ) {
        currencyTransactionRepository.save(CurrencyTransaction.create(
                childId,
                amount,
                balanceAfter,
                reason,
                refType,
                refId
        ));
    }
}
