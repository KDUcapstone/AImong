package com.kduniv.aimong.feature.parent.data.model

import com.google.gson.annotations.SerializedName

data class ParentStageRewardsResponseData(
    @SerializedName("stages") val stages: List<ParentStageRewardDto> = emptyList()
)

data class ParentStageRewardDto(
    @SerializedName("stageNumber") val stageNumber: Int,
    @SerializedName("rewardText") val rewardText: String? = null,
    @SerializedName("isTriggered") val isTriggered: Boolean = false,
    @SerializedName("triggeredAt") val triggeredAt: String? = null,
    @SerializedName("defaultGearReward") val defaultGearReward: Int = 0,
    @SerializedName("normalTicketReward") val normalTicketReward: Int = 0,
    @SerializedName("missionProgress") val missionProgress: ParentStageMissionProgressDto? = null
)

data class ParentStageMissionProgressDto(
    @SerializedName("completed") val completed: Int = 0,
    @SerializedName("total") val total: Int = 0
)

data class CreateParentStageRewardRequest(
    @SerializedName("stageNumber") val stageNumber: Int,
    @SerializedName("rewardText") val rewardText: String
)

data class CreateParentStageRewardResponseData(
    @SerializedName("rewardId") val rewardId: String? = null,
    @SerializedName("stageNumber") val stageNumber: Int,
    @SerializedName("rewardText") val rewardText: String,
    @SerializedName("isTriggered") val isTriggered: Boolean = false
)

data class PatchParentStageRewardRequest(
    @SerializedName("rewardText") val rewardText: String
)

data class PatchParentStageRewardResponseData(
    @SerializedName("stageNumber") val stageNumber: Int,
    @SerializedName("rewardText") val rewardText: String,
    @SerializedName("isTriggered") val isTriggered: Boolean = false
)
