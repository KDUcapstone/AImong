package com.kduniv.aimong.feature.dev.mock

/**
 * [UiMode.useStubNav] 전용 — 퀴즈 부활·보호권 구매·wallet 시트가 공유하는 톱니바퀴 잔액.
 */
object MockGearBalance {
    var gear: Int = 40
        private set

    const val HEART_REVIVE_COST = 10
    const val STREAK_SHIELD_COST = 30

    fun trySpend(amount: Int): Boolean {
        if (gear < amount) return false
        gear -= amount
        return true
    }

    fun credit(amount: Int) {
        gear = (gear + amount).coerceAtLeast(0)
    }

    fun reset() {
        gear = 40
    }
}
