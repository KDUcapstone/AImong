package com.aimong.backend.domain.auth.service;

import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.dto.RegenerateCodeResponse;
import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.repository.ParentAccountRepository;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.AuthHeaderUtils;
import com.aimong.backend.global.util.SecureRandomUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentAuthService {

    private static final String GOOGLE_SIGN_IN_PROVIDER = "google.com";
    private static final int MAX_CODE_RETRY = 5;
    private static final int STARTER_TICKETS = 3;

    private final FirebaseAuth firebaseAuth;
    private final ParentAccountRepository parentAccountRepository;
    private final ChildProfileRepository childProfileRepository;
    private final TicketRepository ticketRepository;
    private final StreakRecordRepository streakRecordRepository;

    @Transactional
    public ParentRegisterResponse register(String authorizationHeader, ParentRegisterRequest request) {
        FirebaseToken firebaseToken = verifyFirebaseToken(authorizationHeader);
        ParentAccount parentAccount = parentAccountRepository.findByFirebaseUid(firebaseToken.getUid())
                .orElseGet(() -> parentAccountRepository.save(
                        ParentAccount.create(firebaseToken.getUid(), firebaseToken.getEmail())
                ));

        ChildProfile childProfile = childProfileRepository.save(
                ChildProfile.create(parentAccount, request.nickname(), generateUniqueCode())
        );
        ticketRepository.save(Ticket.create(childProfile.getId(), STARTER_TICKETS));
        streakRecordRepository.save(StreakRecord.create(childProfile.getId()));

        return new ParentRegisterResponse(
                childProfile.getId(),
                childProfile.getNickname(),
                childProfile.getCode(),
                STARTER_TICKETS
        );
    }

    @Transactional
    public RegenerateCodeResponse regenerateCode(String authorizationHeader, String childId) {
        FirebaseToken firebaseToken = verifyFirebaseToken(authorizationHeader);
        ParentAccount parentAccount = parentAccountRepository.findByFirebaseUid(firebaseToken.getUid())
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));

        ChildProfile childProfile = childProfileRepository.findById(UUID.fromString(childId))
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));

        if (!childProfile.getParentAccount().getId().equals(parentAccount.getId())) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }

        childProfile.regenerateCode(generateUniqueCode());
        return new RegenerateCodeResponse(childProfile.getCode());
    }

    private FirebaseToken verifyFirebaseToken(String authorizationHeader) {
        try {
            String idToken = AuthHeaderUtils.extractBearerToken(authorizationHeader);
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(idToken);
            validateGoogleProvider(firebaseToken);
            return firebaseToken;
        } catch (FirebaseAuthException exception) {
            throw new AimongException(ErrorCode.UNAUTHORIZED, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateGoogleProvider(FirebaseToken firebaseToken) {
        Object firebaseClaim = firebaseToken.getClaims().get("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            throw new AimongException(ErrorCode.UNAUTHORIZED);
        }

        Object signInProvider = firebaseMap.get("sign_in_provider");
        if (!GOOGLE_SIGN_IN_PROVIDER.equals(signInProvider)) {
            throw new AimongException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_RETRY; attempt++) {
            String code = SecureRandomUtils.generateSixDigitCode();
            if (!childProfileRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new AimongException(ErrorCode.CODE_GENERATION_FAILED);
    }
}
