package com.kduniv.aimong.feature.wallet.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.wallet.data.model.GearAddRequest
import com.kduniv.aimong.feature.wallet.data.model.GearAddResponseData
import com.kduniv.aimong.feature.wallet.data.model.WalletResponseData
import com.kduniv.aimong.feature.wallet.domain.model.WalletBalance
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : WalletRepository {

    override suspend fun getWallet(): Result<WalletBalance> {
        return try {
            apiService.getWallet().toResult().map { it.toDomain() }
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addGear(amount: Int): Result<GearAddResponseData> {
        return try {
            apiService.addGear(GearAddRequest(amount)).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun WalletResponseData.toDomain() = WalletBalance(
        gear = gear.coerceAtLeast(0),
        heartReviveCost = costs.heartRevive.coerceAtLeast(0)
            .takeIf { it > 0 } ?: WalletBalance.DEFAULT_HEART_REVIVE_COST,
        streakShieldCost = costs.streakShield.coerceAtLeast(0)
            .takeIf { it > 0 } ?: WalletBalance.DEFAULT_STREAK_SHIELD_COST
    )
}
