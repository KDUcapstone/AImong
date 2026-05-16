package com.kduniv.aimong.feature.streak.data

import com.kduniv.aimong.feature.streak.data.model.StreakShieldPurchaseResponseData
import com.kduniv.aimong.feature.streak.data.model.StreakStatusData

interface StreakRepository {
    suspend fun getStreak(): Result<StreakStatusData>

    suspend fun purchaseShield(count: Int = 1): Result<StreakShieldPurchaseResponseData>
}

