package com.aimong.backend.global.util;

import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;

public final class AuthHeaderUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthHeaderUtils() {
    }

    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AimongException(ErrorCode.UNAUTHORIZED);
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
