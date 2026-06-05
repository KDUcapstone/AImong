package com.kduniv.aimong.feature.quest.data.model

import com.google.gson.annotations.SerializedName

/** GET /child/custom-quests */
data class ChildCustomQuestListResponseData(
    @SerializedName("quests") val quests: List<ChildCustomQuestDto> = emptyList(),
    @SerializedName("hasPendingConfirm") val hasPendingConfirm: Boolean = false,
)

data class ChildCustomQuestDto(
    @SerializedName("questId") val questId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("rewardText") val rewardText: String,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("confirmedAt") val confirmedAt: String? = null,
)

/** POST /child/custom-quests/{questId}/complete */
data class ChildCustomQuestCompleteResponseData(
    @SerializedName("questId") val questId: String,
    @SerializedName("status") val status: String,
    @SerializedName("completedAt") val completedAt: String? = null,
)
