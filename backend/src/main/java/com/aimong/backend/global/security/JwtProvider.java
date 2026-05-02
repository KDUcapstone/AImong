package com.aimong.backend.global.security;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.global.config.JwtProperties;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CHILD_ID_CLAIM = "childId";
    private static final String SESSION_VERSION_CLAIM = "sessionVersion";
    private static final String TYPE_CLAIM = "type";
    private static final String CHILD_TOKEN_TYPE = "CHILD";

    private final ChildProfileRepository childProfileRepository;
    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtProvider(ChildProfileRepository childProfileRepository, JwtProperties jwtProperties) {
        this.childProfileRepository = childProfileRepository;
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createChildSessionToken(String childId, int sessionVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getChildSessionExpiration());

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(childId)
                .claim(CHILD_ID_CLAIM, childId)
                .claim(SESSION_VERSION_CLAIM, sessionVersion)
                .claim(TYPE_CLAIM, CHILD_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public void validateChildSessionToken(String token) {
        Claims claims = parseClaims(token);
        if (!CHILD_TOKEN_TYPE.equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new AimongException(ErrorCode.INVALID_TOKEN);
        }

        UUID childId = UUID.fromString(claims.get(CHILD_ID_CLAIM, String.class));
        Integer tokenSessionVersion = claims.get(SESSION_VERSION_CLAIM, Integer.class);

        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.UNAUTHORIZED));

        if (tokenSessionVersion == null || tokenSessionVersion != childProfile.getSessionVersion()) {
            throw new AimongException(ErrorCode.INVALID_TOKEN);
        }
    }

    public String extractChildId(String token) {
        return parseClaims(token).get(CHILD_ID_CLAIM, String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            throw new AimongException(ErrorCode.TOKEN_EXPIRED, exception);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AimongException(ErrorCode.INVALID_TOKEN, exception);
        }
    }
}
