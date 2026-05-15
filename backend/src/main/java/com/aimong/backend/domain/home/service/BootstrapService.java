package com.aimong.backend.domain.home.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.repository.ParentAccountRepository;
import com.aimong.backend.domain.home.dto.BootstrapResponse;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.security.JwtProvider;
import com.aimong.backend.global.util.AuthHeaderUtils;
import com.aimong.backend.global.util.KstDateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BootstrapService {

    private final FirebaseAuth firebaseAuth;
    private final JwtProvider jwtProvider;
    private final ParentAccountRepository parentAccountRepository;
    private final ChildProfileRepository childProfileRepository;

    @Transactional
    public BootstrapResponse bootstrap(String authorizationHeader) {
        Instant now = Instant.now();
        if (!StringUtils.hasText(authorizationHeader)) {
            return BootstrapResponse.guest(now, KstDateUtils.today());
        }

        String token = AuthHeaderUtils.extractBearerToken(authorizationHeader);
        try {
            return parentBootstrap(token, now);
        } catch (FirebaseAuthException ignored) {
            return childBootstrap(token, now);
        }
    }

    private BootstrapResponse parentBootstrap(String token, Instant now) throws FirebaseAuthException {
        FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token);
        ParentAccount parentAccount = parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseToken.getUid())
                .orElse(null);
        if (parentAccount == null) {
            return new BootstrapResponse(
                    true,
                    "PARENT",
                    new BootstrapResponse.ParentSummary(firebaseToken.getUid(), 0, false),
                    List.of(),
                    null,
                    null,
                    now,
                    KstDateUtils.today(),
                    "1.0.0",
                    false
            );
        }
        List<ChildProfile> children = childProfileRepository
                .findAllByParentAccountParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentAccount.getParentId());
        return new BootstrapResponse(
                true,
                "PARENT",
                new BootstrapResponse.ParentSummary(
                        parentAccount.getParentId(),
                        children.size(),
                        parentAccount.getFcmToken() != null
                ),
                children.stream()
                        .map(child -> new BootstrapResponse.ChildSummary(
                                child.getId(),
                                child.getNickname(),
                                child.getProfileImageType().name(),
                                null,
                                child.getLastActiveAt()
                        ))
                        .toList(),
                null,
                null,
                now,
                KstDateUtils.today(),
                "1.0.0",
                false
        );
    }

    private BootstrapResponse childBootstrap(String token, Instant now) {
        try {
            jwtProvider.validateChildSessionToken(token);
            UUID childId = UUID.fromString(jwtProvider.extractChildId(token));
            ChildProfile childProfile = childProfileRepository.findByIdAndDeletedAtIsNull(childId)
                    .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED, "다시 로그인해 주세요."));
            childProfile.touchLastActiveAt(now);
            return new BootstrapResponse(
                    true,
                    "CHILD",
                    null,
                    null,
                    new BootstrapResponse.ChildSummary(
                            childProfile.getId(),
                            childProfile.getNickname(),
                            childProfile.getProfileImageType().name(),
                            childProfile.getTotalXp(),
                            null
                    ),
                    true,
                    now,
                    KstDateUtils.today(),
                    "1.0.0",
                    false
            );
        } catch (AimongException exception) {
            throw new AimongException(ErrorCode.UNAUTHORIZED, "다시 로그인해 주세요.");
        }
    }
}
