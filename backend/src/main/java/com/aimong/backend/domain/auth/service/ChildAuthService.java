package com.aimong.backend.domain.auth.service;

import com.aimong.backend.domain.auth.dto.ChildLoginRequest;
import com.aimong.backend.domain.auth.dto.ChildLoginResponse;
import com.aimong.backend.domain.auth.dto.FcmTokenRequest;
import com.aimong.backend.domain.auth.dto.FcmTokenResponse;
import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.support.LoginAttemptService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.security.JwtProvider;
import com.aimong.backend.global.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChildAuthService {

    private final ChildProfileRepository childProfileRepository;
    private final JwtProvider jwtProvider;
    private final LoginAttemptService loginAttemptService;
    private final ChildActivityService childActivityService;

    @Transactional
    public ChildLoginResponse login(ChildLoginRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = ClientIpUtils.extractClientIp(httpServletRequest);
        loginAttemptService.validateNotLocked(clientIp, request.code());

        ChildProfile childProfile = childProfileRepository.findByCode(request.code())
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(clientIp, request.code());
                    return new AimongException(ErrorCode.CHILD_CODE_NOT_FOUND);
                });

        String sessionToken = jwtProvider.createChildSessionToken(
                childProfile.getId().toString(),
                childProfile.getSessionVersion()
        );
        childProfile.touchLastActiveAt(Instant.now());
        loginAttemptService.recordSuccess(clientIp, request.code());

        return new ChildLoginResponse(
                childProfile.getId(),
                childProfile.getNickname(),
                sessionToken,
                childProfile.getProfileImageType().name(),
                childProfile.getTotalXp()
        );
    }

    @Transactional
    public FcmTokenResponse registerFcmToken(UUID childId, FcmTokenRequest request) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        childProfile.updateFcmToken(request.fcmToken());
        childActivityService.touchLastActiveAt(childId);
        return new FcmTokenResponse(true);
    }
}
