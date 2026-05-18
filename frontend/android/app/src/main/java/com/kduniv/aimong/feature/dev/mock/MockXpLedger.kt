package com.kduniv.aimong.feature.dev.mock

/**
 * [UiMode.useStubNav] + [MockHomeFragment] 전용 — 퀴즈 제출 XP를 홈 칩·펫 바에 반영.
 */
object MockXpLedger {
    var userTotalXp: Int = 1520
        private set
    var petXp: Int = 8
        private set

    fun applyMissionReward(xpEarned: Int, petXpAfter: Int?) {
        if (xpEarned > 0) userTotalXp += xpEarned
        when {
            petXpAfter != null && petXpAfter > 0 -> petXp = petXpAfter
            xpEarned > 0 -> petXp += xpEarned
        }
    }
}
