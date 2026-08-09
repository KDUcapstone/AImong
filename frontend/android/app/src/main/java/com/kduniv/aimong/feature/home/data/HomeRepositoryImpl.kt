package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.runApiCall
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.dev.mock.ChildStageRewardsStub
import com.kduniv.aimong.feature.home.data.model.ChildStageRewardsResponseData
import com.kduniv.aimong.feature.home.data.model.EnergyAddRequest
import com.kduniv.aimong.feature.home.data.model.EnergyAddResponseData
import com.kduniv.aimong.feature.home.data.model.EnergyStateData
import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardCheckResponseData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardClaimResponseData
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : HomeRepository {

    override suspend fun getHome(): Result<HomeScreenData> = runApiCall {
        apiService.getHome().toResult().getOrThrow()
    }

    override suspend fun getEnergy(): Result<EnergyStateData> = runApiCall {
        apiService.getEnergy().toResult().getOrThrow()
    }

    override suspend fun addEnergy(amount: Int): Result<EnergyAddResponseData> = runApiCall {
        apiService.addEnergy(EnergyAddRequest(amount)).toResult().getOrThrow()
    }

    override suspend fun getStreakCalendar(yearMonth: String?): Result<StreakCalendarResult> = runApiCall {
        StreakCalendarMapper.normalize(yearMonth, apiService.getStreakCalendar(yearMonth).toResult().getOrThrow())
    }

    override suspend fun getReturnReward(): Result<ReturnRewardCheckResponseData> = runApiCall {
        apiService.getReturnReward().toResult().getOrThrow()
    }

    override suspend fun claimReturnReward(): Result<ReturnRewardClaimResponseData> = runApiCall {
        apiService.claimReturnReward().toResult().getOrThrow()
    }

    override suspend fun getStageRewards(): Result<ChildStageRewardsResponseData> {
        if (UiMode.useStubNav) {
            return Result.success(ChildStageRewardsStub.get())
        }
        return runApiCall {
            apiService.getChildStageRewards().toResult().getOrThrow()
        }
    }
}
