package com.kduniv.aimong.feature.home.domain.repository

import com.kduniv.aimong.feature.home.data.model.EnergyAddResponseData
import com.kduniv.aimong.feature.home.data.model.EnergyStateData
import com.kduniv.aimong.feature.home.data.model.ChildStageRewardsResponseData
import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardCheckResponseData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardClaimResponseData
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult

interface HomeRepository {
    suspend fun getHome(): Result<HomeScreenData>

    suspend fun getEnergy(): Result<EnergyStateData>

    suspend fun addEnergy(amount: Int): Result<EnergyAddResponseData>

    /**
     * GET /home/streak-calendar
     * @param yearMonth `YYYY-MM`. null이면 서버가 KST 현재 월 사용.
     */
    suspend fun getStreakCalendar(yearMonth: String? = null): Result<StreakCalendarResult>

    /** GET /return-reward — CHILD */
    suspend fun getReturnReward(): Result<ReturnRewardCheckResponseData>

    /** POST /return-reward/claim — CHILD (400 BAD_REQUEST, 409 CONFLICT 등) */
    suspend fun claimReturnReward(): Result<ReturnRewardClaimResponseData>

    /** GET /child/stage-rewards — 홈 단계별 보상 보물상자 */
    suspend fun getStageRewards(): Result<ChildStageRewardsResponseData>
}
