package com.kduniv.aimong.feature.home.data.model

import com.google.gson.annotations.SerializedName

/** GET /child/stage-rewards — 자녀 홈 단계별 보상 조회 */
data class ChildStageRewardsResponseData(
    @SerializedName("stages") val stages: List<ChildStageRewardDto> = emptyList(),
)

data class ChildStageRewardDto(
    @SerializedName("stageNumber") val stageNumber: Int,
    @SerializedName("rewardText") val rewardText: String? = null,
    @SerializedName("isTriggered") val isTriggered: Boolean = false,
    @SerializedName("triggeredAt") val triggeredAt: String? = null,
    @SerializedName("defaultGearReward") val defaultGearReward: Int = 0,
    @SerializedName("normalTicketReward") val normalTicketReward: Int = 0,
)
