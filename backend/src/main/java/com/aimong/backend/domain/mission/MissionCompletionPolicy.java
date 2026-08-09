package com.aimong.backend.domain.mission;

public final class MissionCompletionPolicy {

    public static final int PASS_SCORE_NUMERATOR = 8;
    public static final int PASS_SCORE_DENOMINATOR = 10;

    private MissionCompletionPolicy() {
    }

    public static boolean isPassed(int score, int total) {
        return score * PASS_SCORE_DENOMINATOR >= total * PASS_SCORE_NUMERATOR;
    }
}
