package com.aimong.backend.domain.home.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.home.dto.DevEnergyAddRequest;
import com.aimong.backend.domain.home.dto.DevEnergyAddResponse;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "dev", "test"})
@RequiredArgsConstructor
public class DevEnergyService {

    private final ChildProfileRepository childProfileRepository;

    @Transactional
    public DevEnergyAddResponse add(UUID childId, DevEnergyAddRequest request) {
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));

        Instant now = Instant.now();
        childProfile.recoverEnergy(now);
        int before = childProfile.getEnergy();
        childProfile.addEnergy(request.amount(), now);
        Instant nextRecoverAt = childProfile.nextEnergyRecoverAt();

        return new DevEnergyAddResponse(
                childProfile.getEnergy(),
                ChildProfile.MAX_ENERGY,
                childProfile.getEnergy() - before,
                nextRecoverAt,
                fullRecoverAt(childProfile, nextRecoverAt)
        );
    }

    private Instant fullRecoverAt(ChildProfile childProfile, Instant nextRecoverAt) {
        if (nextRecoverAt == null) {
            return null;
        }
        int missingEnergy = ChildProfile.MAX_ENERGY - childProfile.getEnergy();
        return childProfile.getEnergyRecoveredAt()
                .plusSeconds((long) missingEnergy * ChildProfile.ENERGY_RECOVERY_MINUTES * 60L);
    }
}
