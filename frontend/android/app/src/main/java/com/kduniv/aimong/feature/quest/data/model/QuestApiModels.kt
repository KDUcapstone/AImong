package com.kduniv.aimong.feature.quest.data.model

import com.google.gson.annotations.SerializedName

data class DailyQuestsResponseData(
    @SerializedName("date") val date: String? = null,
    @SerializedName("todayXp") val todayXp: Int? = null,
    @SerializedName("quests") val quests: List<QuestApiItemDto> = emptyList()
)

data class WeeklyQuestsResponseData(
    @SerializedName("weekStart") val weekStart: String? = null,
    @SerializedName("weeklyXp") val weeklyXp: Int? = null,
    @SerializedName("quests") val quests: List<QuestApiItemDto> = emptyList()
)

data class QuestApiItemDto(
    @SerializedName("questType") val questType: String,
    @SerializedName("label") val label: String,
    @SerializedName("reward") val reward: String = "",
    @SerializedName("claimType") val claimType: String,
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("rewardClaimed") val rewardClaimed: Boolean = false,
    @SerializedName("progress") val progress: QuestApiProgressDto
)

data class QuestApiProgressDto(
    @SerializedName("current") val current: Int = 0,
    @SerializedName("required") val required: Int = 0
)

data class QuestClaimRequest(
    @SerializedName("questType") val questType: String,
    @SerializedName("period") val period: String
)

data class QuestClaimResponseData(
    @SerializedName("rewards") val rewards: List<QuestRewardItemDto> = emptyList(),
    @SerializedName("remainingTickets") val remainingTickets: QuestRemainingTicketsDto
)

data class QuestRewardItemDto(
    @SerializedName("type") val type: String,
    @SerializedName("ticketType") val ticketType: String? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("reason") val reason: String? = null
)

data class QuestRemainingTicketsDto(
    @SerializedName("normal") val normal: Int = 0,
    @SerializedName("rare") val rare: Int = 0,
    @SerializedName("epic") val epic: Int = 0
)
