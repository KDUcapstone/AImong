package com.aimong.backend.domain.reward.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.reward.dto.WalletResponse;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        return new WalletResponse(
                childProfile.getGear(),
                new WalletResponse.CostsResponse(
                        CurrencyService.HEART_REVIVE_COST,
                        CurrencyService.STREAK_SHIELD_COST
                )
        );
    }
}
