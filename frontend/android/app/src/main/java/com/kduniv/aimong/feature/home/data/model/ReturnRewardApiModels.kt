package com.kduniv.aimong.feature.home.data.model

import com.google.gson.annotations.SerializedName

data class ReturnRewardCheckResponseData(
    @SerializedName("hasReward") val hasReward: Boolean = false,
    @SerializedName("daysMissed") val daysMissed: Int? = null,
    @SerializedName("ticketCount") val ticketCount: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class ReturnRewardClaimResponseData(
    /** 명세 v1 기본 형태. v1.1 일부 응답은 [rewards]만 올 수 있음. */
    @SerializedName("ticketEarned") val ticketEarned: ReturnRewardTicketEarnedDto? = null,
    @SerializedName("rewards") val rewards: List<ReturnRewardItemDto> = emptyList(),
    @SerializedName("remainingTickets") val remainingTickets: ReturnRewardRemainingTicketsDto? = null
)

data class ReturnRewardTicketEarnedDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("count") val count: Int = 0
)

data class ReturnRewardItemDto(
    @SerializedName("type") val type: String,
    @SerializedName("ticketType") val ticketType: String? = null,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("reason") val reason: String? = null
)

data class ReturnRewardRemainingTicketsDto(
    @SerializedName("normal") val normal: Int = 0,
)

