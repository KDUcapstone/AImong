package com.aimong.backend.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.config.JwtProperties;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    @Mock
    private ChildProfileRepository childProfileRepository;

    @Test
    void validateChildSessionTokenRejectsTokenAfterCodeRegeneration() {
        JwtProvider jwtProvider = new JwtProvider(childProfileRepository, jwtProperties());
        ChildProfile childProfile = ChildProfile.create(
                ParentAccount.create("firebase-parent", "parent@example.com"),
                "test-child",
                "123456"
        );
        String staleToken = jwtProvider.createChildSessionToken(childProfile.getId().toString(), childProfile.getSessionVersion());
        childProfile.regenerateCode("654321");
        when(childProfileRepository.findById(childProfile.getId())).thenReturn(Optional.of(childProfile));

        assertThatThrownBy(() -> jwtProvider.validateChildSessionToken(staleToken))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setIssuer("aimong-test");
        properties.setChildSessionExpiration(30L * 24 * 60 * 60 * 1000);
        return properties;
    }
}
