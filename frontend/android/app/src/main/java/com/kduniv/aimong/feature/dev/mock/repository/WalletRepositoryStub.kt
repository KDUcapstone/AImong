package com.kduniv.aimong.feature.dev.mock.repository

import com.kduniv.aimong.feature.dev.mock.MockGearBalance
import com.kduniv.aimong.feature.wallet.data.model.GearAddResponseData
import com.kduniv.aimong.feature.wallet.domain.model.WalletBalance
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryStub @Inject constructor() : WalletRepository {

    override suspend fun getWallet(): Result<WalletBalance> = Result.success(
        WalletBalance(
            gear = MockGearBalance.gear,
            heartReviveCost = MockGearBalance.HEART_REVIVE_COST,
            streakShieldCost = MockGearBalance.STREAK_SHIELD_COST
        )
    )

    override suspend fun addGear(amount: Int): Result<GearAddResponseData> {
        MockGearBalance.credit(amount)
        return Result.success(
            GearAddResponseData(
                gear = MockGearBalance.gear,
                addedGear = amount,
            )
        )
    }
}
