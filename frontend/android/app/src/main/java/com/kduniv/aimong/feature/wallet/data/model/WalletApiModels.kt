package com.kduniv.aimong.feature.wallet.data.model

import com.google.gson.annotations.SerializedName

/** GET /wallet */
data class WalletResponseData(
    @SerializedName("gear") val gear: Int,
    @SerializedName("costs") val costs: WalletCostsDto
)

data class WalletCostsDto(
    @SerializedName("heartRevive") val heartRevive: Int = 10,
    @SerializedName("streakShield") val streakShield: Int = 30
)

data class GearAddRequest(
    @SerializedName("amount") val amount: Int
)

/** POST /wallet/add 의 `data` */
data class GearAddResponseData(
    @SerializedName("gear") val gear: Int = 0,
    @SerializedName("addedGear") val addedGear: Int? = null,
)
