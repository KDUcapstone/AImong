package com.aimong.backend.domain.auth.service;

import com.aimong.backend.domain.auth.dto.DeleteChildResponse;
import com.aimong.backend.domain.auth.dto.DeleteFcmTokenResponse;
import com.aimong.backend.domain.auth.dto.FcmTokenRequest;
import com.aimong.backend.domain.auth.dto.FcmTokenResponse;
import com.aimong.backend.domain.auth.dto.LogoutResponse;
import com.aimong.backend.domain.auth.dto.ParentChildDetailResponse;
import com.aimong.backend.domain.auth.dto.ParentChildrenResponse;
import com.aimong.backend.domain.auth.dto.ParentMeResponse;
import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.dto.RegenerateCodeResponse;
import com.aimong.backend.domain.auth.dto.UpdateChildProfileRequest;
import com.aimong.backend.domain.auth.dto.UpdateChildProfileResponse;
import com.aimong.backend.domain.auth.dto.WithdrawParentRequest;
import com.aimong.backend.domain.auth.dto.WithdrawParentResponse;
import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.repository.ParentAccountRepository;
import com.aimong.backend.domain.auth.repository.ParentNotificationSettingsRepository;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.SecureRandomUtils;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentAuthService {

    private static final int MAX_CODE_RETRY = 5;
    private static final int STARTER_TICKETS = 3;
    private static final int MAX_CHILDREN_PER_PARENT = 3;

    private final ParentAccountRepository parentAccountRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ParentNotificationSettingsRepository parentNotificationSettingsRepository;
    private final TicketRepository ticketRepository;
    private final StreakRecordRepository streakRecordRepository;

    @Transactional
    public ParentRegisterResponse register(String firebaseUid, String firebaseEmail, ParentRegisterRequest request) {
        ParentAccount parentAccount = parentAccountRepository.findWithLockByParentId(firebaseUid)
                .orElseGet(() -> parentAccountRepository.save(
                        ParentAccount.create(firebaseUid, firebaseEmail)
                ));
        refreshParentAccount(parentAccount, firebaseEmail);
        ensureNotificationSettings(parentAccount.getParentId());
        return createChild(parentAccount, request.nickname());
    }

    @Transactional
    public ParentRegisterResponse addChild(String firebaseUid, String firebaseEmail, ParentRegisterRequest request) {
        ParentAccount parentAccount = parentAccountRepository.findWithLockByParentId(firebaseUid)
                .orElseGet(() -> parentAccountRepository.save(ParentAccount.create(firebaseUid, firebaseEmail)));
        refreshParentAccount(parentAccount, firebaseEmail);
        ensureNotificationSettings(parentAccount.getParentId());
        return createChild(parentAccount, request.nickname());
    }

    private ParentRegisterResponse createChild(ParentAccount parentAccount, String nickname) {
        if (childProfileRepository.countByParentAccountParentId(parentAccount.getParentId()) >= MAX_CHILDREN_PER_PARENT) {
            throw new AimongException(ErrorCode.CHILD_LIMIT_EXCEEDED);
        }

        ChildProfile childProfile = childProfileRepository.save(
                ChildProfile.create(parentAccount, nickname, generateUniqueCode())
        );
        ticketRepository.saveAll(IntStream.range(0, STARTER_TICKETS)
                .mapToObj(index -> Ticket.issue(childProfile.getId(), TicketType.NORMAL))
                .toList());
        streakRecordRepository.save(StreakRecord.create(childProfile.getId()));
        childProfile.markStarterIssued();

        return new ParentRegisterResponse(
                childProfile.getId(),
                childProfile.getNickname(),
                childProfile.getCode(),
                STARTER_TICKETS
        );
    }

    @Transactional
    public RegenerateCodeResponse regenerateCode(String firebaseUid, String childId) {
        ParentAccount parentAccount = parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));

        ChildProfile childProfile = childProfileRepository.findWithLockById(parseChildId(childId))
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));

        if (!childProfile.getParentAccount().getParentId().equals(parentAccount.getParentId())) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }

        childProfile.regenerateCode(generateUniqueCode());
        return new RegenerateCodeResponse(childProfile.getCode());
    }

    @Transactional
    public FcmTokenResponse registerFcmToken(String firebaseUid, FcmTokenRequest request) {
        ParentAccount parentAccount = parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));
        parentAccount.updateFcmToken(request.fcmToken());
        return new FcmTokenResponse(true);
    }

    @Transactional
    public DeleteFcmTokenResponse deleteFcmToken(String firebaseUid) {
        ParentAccount parentAccount = parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));
        parentAccount.clearFcmToken();
        return new DeleteFcmTokenResponse(true);
    }

    @Transactional
    public LogoutResponse logout(String firebaseUid) {
        parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .ifPresent(ParentAccount::clearFcmToken);
        return new LogoutResponse(true);
    }

    @Transactional(readOnly = true)
    public ParentChildrenResponse getChildren(String firebaseUid) {
        return parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .map(parentAccount -> new ParentChildrenResponse(
                        childProfileRepository.findAllByParentAccountParentIdOrderByCreatedAtAsc(parentAccount.getParentId()).stream()
                                .map(childProfile -> new ParentChildrenResponse.ChildSummary(
                                        childProfile.getId(),
                                        childProfile.getNickname(),
                                        childProfile.getCode(),
                                        childProfile.getProfileImageType().name(),
                                        childProfile.getTotalXp()
                                ))
                                .toList()
                ))
                .orElseGet(() -> new ParentChildrenResponse(java.util.List.of()));
    }

    @Transactional(readOnly = true)
    public ParentMeResponse getMe(String firebaseUid) {
        ParentAccount parentAccount = parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AimongException(ErrorCode.PARENT_NOT_FOUND));
        return new ParentMeResponse(
                parentAccount.getParentId(),
                parentAccount.getEmail(),
                parentAccount.getFcmToken() != null,
                childProfileRepository.countByParentAccountParentId(parentAccount.getParentId()),
                parentAccount.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ParentChildDetailResponse getChild(String firebaseUid, String childId) {
        ChildProfile childProfile = ownedChild(firebaseUid, parseChildId(childId));
        return new ParentChildDetailResponse(
                childProfile.getId(),
                childProfile.getNickname(),
                childProfile.getCode(),
                childProfile.getProfileImageType().name(),
                childProfile.getTotalXp(),
                childProfile.getFcmToken() != null,
                childProfile.getLastActiveAt(),
                childProfile.getCreatedAt()
        );
    }

    @Transactional
    public UpdateChildProfileResponse updateChild(String firebaseUid, String childId, UpdateChildProfileRequest request) {
        if (request == null || request.hasNoValues()) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "수정할 값을 입력해 주세요.");
        }
        if (request.nickname() != null && request.nickname().isBlank()) {
            throw new AimongException(ErrorCode.NICKNAME_REQUIRED);
        }
        ChildProfile childProfile = ownedChild(firebaseUid, parseChildId(childId));
        childProfile.updateProfile(
                request.nickname() == null ? null : request.nickname().trim(),
                request.profileImageType()
        );
        return new UpdateChildProfileResponse(
                childProfile.getId(),
                childProfile.getNickname(),
                childProfile.getProfileImageType().name()
        );
    }

    @Transactional
    public DeleteChildResponse deleteChild(String firebaseUid, String childId) {
        ChildProfile childProfile = ownedChild(firebaseUid, parseChildId(childId));
        childProfile.softDelete(Instant.now());
        return new DeleteChildResponse(true);
    }

    @Transactional
    public WithdrawParentResponse withdraw(String firebaseUid, WithdrawParentRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.confirm())) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "회원탈퇴 확인이 필요합니다.");
        }
        ParentAccount parentAccount = parentAccountRepository.findWithLockByParentIdAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));
        Instant now = Instant.now();
        childProfileRepository.findAllByParentAccountParentId(parentAccount.getParentId())
                .forEach(child -> child.softDelete(now));
        parentAccount.withdraw(now);
        return new WithdrawParentResponse(true);
    }

    private ChildProfile ownedChild(String firebaseUid, UUID childId) {
        ParentAccount parentAccount = parentAccountRepository.findByParentIdAndDeletedAtIsNull(firebaseUid)
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));
        ChildProfile childProfile = childProfileRepository.findByIdAndDeletedAtIsNull(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!childProfile.getParentAccount().getParentId().equals(parentAccount.getParentId())) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        return childProfile;
    }

    private void refreshParentAccount(ParentAccount parentAccount, String firebaseEmail) {
        if (parentAccount.isDeleted()) {
            parentAccount.reactivate(firebaseEmail);
            return;
        }
        parentAccount.updateEmail(firebaseEmail);
    }

    private void ensureNotificationSettings(String parentId) {
        if (parentNotificationSettingsRepository == null) {
            return;
        }
        if (!parentNotificationSettingsRepository.existsById(parentId)) {
            parentNotificationSettingsRepository.save(
                    com.aimong.backend.domain.auth.entity.ParentNotificationSettings.createDefault(parentId)
            );
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_RETRY; attempt++) {
            String code = SecureRandomUtils.generateSixDigitCode();
            if (!childProfileRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new AimongException(ErrorCode.CODE_GENERATION_FAILED_WITH_RETRY);
    }

    private UUID parseChildId(String childId) {
        try {
            return UUID.fromString(childId);
        } catch (IllegalArgumentException exception) {
            throw new AimongException(ErrorCode.BAD_REQUEST, exception);
        }
    }
}
