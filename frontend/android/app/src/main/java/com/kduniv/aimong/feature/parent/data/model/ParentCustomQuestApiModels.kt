package com.kduniv.aimong.feature.parent.data.model

import com.google.gson.annotations.SerializedName

data class ParentCustomQuestListResponseData(
    @SerializedName("quests") val quests: List<ParentCustomQuestDto> = emptyList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 0,
    @SerializedName(value = "totalCount", alternate = ["totalElements"])
    val totalCount: Int = 0,
    @SerializedName("hasNext") val hasNext: Boolean = false
)

data class ParentCustomQuestDto(
    @SerializedName("questId") val questId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("rewardText") val rewardText: String,
    @SerializedName("status") val status: String,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("confirmedAt") val confirmedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class CreateParentCustomQuestRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("rewardText") val rewardText: String,
    @SerializedName("expiresAt") val expiresAt: String? = null
)

data class CreateParentCustomQuestResponseData(
    @SerializedName("questId") val questId: String,
    @SerializedName("status") val status: String,
    @SerializedName("title") val title: String,
    @SerializedName("rewardText") val rewardText: String,
    @SerializedName("expiresAt") val expiresAt: String? = null
)

data class ConfirmParentCustomQuestResponseData(
    @SerializedName("questId") val questId: String,
    @SerializedName("status") val status: String,
    @SerializedName("confirmedAt") val confirmedAt: String? = null
)

data class CancelParentCustomQuestResponseData(
    @SerializedName("questId") val questId: String,
    @SerializedName("status") val status: String
)
