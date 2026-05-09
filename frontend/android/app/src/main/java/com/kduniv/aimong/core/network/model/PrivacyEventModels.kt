package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

data class PrivacyEventRequest(
    @SerializedName("detectedType") val detectedType: String,
    @SerializedName("masked") val masked: Boolean
)

data class PrivacyEventResponseData(
    @SerializedName("recorded") val recorded: Boolean = false
)
