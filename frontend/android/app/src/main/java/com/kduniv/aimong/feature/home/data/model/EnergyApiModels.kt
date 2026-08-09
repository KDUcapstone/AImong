package com.kduniv.aimong.feature.home.data.model

import com.google.gson.annotations.SerializedName

/** GET /energy 의 `data` */
data class EnergyStateData(
    @SerializedName("energy") val energy: Int = 0,
    @SerializedName("maxEnergy") val maxEnergy: Int = 20,
    @SerializedName("nextEnergyRecoverAt") val nextEnergyRecoverAt: String? = null,
    @SerializedName("fullRecoverAt") val fullRecoverAt: String? = null,
    @SerializedName("recoverIntervalMinutes") val recoverIntervalMinutes: Int? = null,
    @SerializedName("missionStartCost") val missionStartCost: Int? = null
)

data class EnergyAddRequest(
    @SerializedName("amount") val amount: Int
)

/** POST /energy/add 의 `data` */
data class EnergyAddResponseData(
    @SerializedName("energy") val energy: Int = 0,
    @SerializedName("maxEnergy") val maxEnergy: Int = 20,
    @SerializedName("addedEnergy") val addedEnergy: Int? = null,
    @SerializedName("nextEnergyRecoverAt") val nextEnergyRecoverAt: String? = null,
    @SerializedName("fullRecoverAt") val fullRecoverAt: String? = null
)
