package com.kduniv.aimong.feature.wallet.domain.model

/** 계정 단위 톱니바퀴 잔액 및 주요 사용처 비용 */
data class WalletBalance(
    val gear: Int,
    val heartReviveCost: Int,
    val streakShieldCost: Int
) {
    companion object {
        const val DEFAULT_HEART_REVIVE_COST = 10
        const val DEFAULT_STREAK_SHIELD_COST = 30
    }
}
