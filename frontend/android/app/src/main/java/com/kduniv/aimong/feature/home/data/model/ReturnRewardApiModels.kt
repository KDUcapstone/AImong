package com.kduniv.aimong.feature.home.data.model

import com.google.gson.annotations.SerializedName

data class ReturnRewardCheckResponseData(
    @SerializedName("hasReward") val hasReward: Boolean = false,
    @SerializedName("daysMissed") val daysMissed: Int? = null,
    @SerializedName("ticketCount") val ticketCount: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class ReturnRewardClaimResponseData(
    @SerializedName("rewards") val rewards: List<ReturnRewardItemDto> = emptyList(),
    @SerializedName("remainingTickets") val remainingTickets: ReturnRewardRemainingTicketsDto
)

data class ReturnRewardItemDto(
    @SerializedName("type") val type: String,
    @SerializedName("ticketType") val ticketType: String? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("reason") val reason: String? = null
)

data class ReturnRewardRemainingTicketsDto(
    @SerializedName("normal") val normal: Int = 0,
    @SerializedName("rare") val rare: Int = 0,
    @SerializedName("epic") val epic: Int = 0
)

