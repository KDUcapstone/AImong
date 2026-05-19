package com.kduniv.aimong.feature.gacha.data.model

import com.google.gson.annotations.SerializedName

/** v2.2: 유일한 티켓 종류 */
object GachaTicketType {
    const val NORMAL = "NORMAL"
}

data class GachaPullRequest(
    @SerializedName("ticketType") val ticketType: String = GachaTicketType.NORMAL,
)

data class GachaPullResultDto(
    @SerializedName("petId") val petId: String,
    @SerializedName("petType") val petType: String,
    @SerializedName("petName") val petName: String? = null,
    @SerializedName("grade") val grade: String,
    @SerializedName("isNew") val isNew: Boolean,
    @SerializedName("fragmentsGot") val fragmentsGot: Int
)

/** v2.2: `POST /gacha/pull` 응답 — 기본 티켓만 */
data class RemainingTicketsDto(
    @SerializedName("normal") val normal: Int = 0,
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
