package com.kduniv.aimong.feature.gacha.data.model

import com.google.gson.annotations.SerializedName

data class GachaPullRequest(
    @SerializedName("ticketType") val ticketType: String
)

data class GachaPullResultDto(
    @SerializedName("petId") val petId: String,
    @SerializedName("petType") val petType: String,
    @SerializedName("petName") val petName: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("isNew") val isNew: Boolean,
    @SerializedName("fragmentsGot") val fragmentsGot: Int
)

data class RemainingTicketsDto(
    @SerializedName("normal") val normal: Int,
    @SerializedName("rare") val rare: Int,
    @SerializedName("epic") val epic: Int
)

data class GachaPullData(
    @SerializedName("result") val result: GachaPullResultDto,
    @SerializedName("srMissCount") val srMissCount: Int,
    @SerializedName("srBonus") val srBonus: Double,
    @SerializedName("levelUp") val levelUp: Boolean,
    @SerializedName("remainingTickets") val remainingTickets: RemainingTicketsDto
)

data class FragmentGradeRow(
    @SerializedName("grade") val grade: String,
    @SerializedName("count") val count: Int,
    @SerializedName("exchangeThreshold") val exchangeThreshold: Int
)

data class GachaFragmentsData(
    @SerializedName("fragments") val fragments: List<FragmentGradeRow>
)

data class GachaExchangeRequest(
    @SerializedName("grade") val grade: String,
    @SerializedName("petType") val petType: String
)

data class GachaExchangeData(
    @SerializedName("petId") val petId: String,
    @SerializedName("petType") val petType: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("stage") val stage: String
)
