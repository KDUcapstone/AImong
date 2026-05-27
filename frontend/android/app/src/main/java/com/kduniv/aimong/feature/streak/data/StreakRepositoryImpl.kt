package com.kduniv.aimong.feature.streak.data

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.core.network.runApi
import com.kduniv.aimong.feature.dev.mock.MockGearBalance
import com.kduniv.aimong.feature.streak.data.model.StreakPartnerDto
import com.kduniv.aimong.feature.streak.data.model.StreakShieldPurchaseRequest
import com.kduniv.aimong.feature.streak.data.model.StreakShieldPurchaseResponseData
import com.kduniv.aimong.feature.streak.data.model.StreakShieldUseResponseData
import com.kduniv.aimong.feature.streak.data.model.StreakStatusData
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : StreakRepository {

    private var stubShieldCount = 2

    override suspend fun getStreak(): Result<StreakStatusData> {
        if (UiMode.useStubNav) {
            return Result.success(
                StreakStatusData(
                    continuousDays = 5,
                    lastCompletedDate = "2026-03-28",
                    todaySetCount = 1,
                    shieldCount = stubShieldCount,
                    status = "ACTIVE",
                    partner = StreakPartnerDto(
                        childId = "stub-partner",
                        nickname = "지우",
                        todayCompleted = true
                    )
                )
            )
        }
        return runApi { apiService.getStreak() }
    }

    override suspend fun purchaseShield(count: Int): Result<StreakShieldPurchaseResponseData> {
        val safeCount = count.coerceAtLeast(1)
        if (UiMode.useStubNav) {
            val totalCost = MockGearBalance.STREAK_SHIELD_COST * safeCount
            if (!MockGearBalance.trySpend(totalCost)) {
                return Result.failure(Exception("톱니바퀴가 부족해요."))
            }
            stubShieldCount += safeCount
            return Result.success(
                StreakShieldPurchaseResponseData(
                    shieldCount = stubShieldCount,
                    gearBalance = MockGearBalance.gear
                )
            )
        }
        return try {
            apiService.purchaseStreakShield(StreakShieldPurchaseRequest(safeCount)).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun useShield(): Result<StreakShieldUseResponseData> {
        if (UiMode.useStubNav) {
            if (stubShieldCount <= 0) {
                return Result.failure(Exception("사용할 불꽃 방패가 없어요."))
            }
            stubShieldCount -= 1
            return Result.success(
                StreakShieldUseResponseData(
                    continuousDays = 5,
                    shieldCount = stubShieldCount,
                    status = "PROTECTED",
                    used = true,
                ),
            )
        }
        return try {
            apiService.useStreakShield().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
