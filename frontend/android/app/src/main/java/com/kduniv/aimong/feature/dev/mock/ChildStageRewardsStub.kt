package com.kduniv.aimong.feature.dev.mock

import com.kduniv.aimong.feature.home.data.model.ChildStageRewardDto
import com.kduniv.aimong.feature.home.data.model.ChildStageRewardsResponseData

/** [com.kduniv.aimong.core.dev.UiMode.useStubNav] 홈 단계 보상 목업 */
object ChildStageRewardsStub {

    fun get(): ChildStageRewardsResponseData = ChildStageRewardsResponseData(
        stages = listOf(
            ChildStageRewardDto(
                stageNumber = 1,
                rewardText = "아이스크림 사주기",
                defaultGearReward = 30,
                normalTicketReward = 0,
            ),
            ChildStageRewardDto(
                stageNumber = 2,
                rewardText = null,
                defaultGearReward = 50,
                normalTicketReward = 0,
            ),
            ChildStageRewardDto(
                stageNumber = 3,
                rewardText = "영화 관람",
                defaultGearReward = 80,
                normalTicketReward = 3,
            ),
        ),
    )
}
