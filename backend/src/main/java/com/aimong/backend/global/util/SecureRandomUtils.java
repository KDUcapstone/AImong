package com.aimong.backend.global.util;

import java.security.SecureRandom;

public final class SecureRandomUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureRandomUtils() {
    }

    public static String generateSixDigitCode() {
        return "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
    }
}
