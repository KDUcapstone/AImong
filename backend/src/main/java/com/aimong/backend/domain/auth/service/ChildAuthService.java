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
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChildAuthService {

    private static final Pattern CHILD_LOGIN_CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final String BLANK_LOGIN_ATTEMPT_KEY = "<blank>";

    private final ChildProfileRepository childProfileRepository;
    private final JwtProvider jwtProvider;
    private final LoginAttemptService loginAttemptService;
    private final ChildActivityService childActivityService;

    @Transactional
    public ChildLoginResponse login(ChildLoginRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = ClientIpUtils.extractClientIp(httpServletRequest);
        String code = request == null || request.code() == null ? "" : request.code();
        String attemptKey = code.isBlank() ? BLANK_LOGIN_ATTEMPT_KEY : code;
        loginAttemptService.validateNotLocked(clientIp, attemptKey);

        if (code.isBlank()) {
            loginAttemptService.recordFailure(clientIp, attemptKey);
            throw new AimongException(ErrorCode.CHILD_CODE_REQUIRED);
        }

        if (!CHILD_LOGIN_CODE_PATTERN.matcher(code).matches()) {
            loginAttemptService.recordFailure(clientIp, attemptKey);
            throw new AimongException(ErrorCode.CHILD_CODE_INVALID_FORMAT);
        }

        ChildProfile childProfile = childProfileRepository.findByCode(code)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(clientIp, attemptKey);
                    return new AimongException(ErrorCode.CHILD_CODE_NOT_FOUND);
                });

        String sessionToken = jwtProvider.createChildSessionToken(
                childProfile.getId().toString(),
                childProfile.getSessionVersion()
        );
        childProfile.touchLastActiveAt(Instant.now());
        loginAttemptService.recordSuccess(clientIp, attemptKey);

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
