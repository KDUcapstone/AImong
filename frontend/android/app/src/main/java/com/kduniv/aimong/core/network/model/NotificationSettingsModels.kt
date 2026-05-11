package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

data class NotificationSettingsRequest(
    @SerializedName("privacyAlertEnabled") val privacyAlertEnabled: Boolean? = null,
    @SerializedName("studyReminderEnabled") val studyReminderEnabled: Boolean? = null,
    @SerializedName("returnRewardEnabled") val returnRewardEnabled: Boolean? = null,
    @SerializedName("questRewardEnabled") val questRewardEnabled: Boolean? = null,
    @SerializedName("marketingEnabled") val marketingEnabled: Boolean? = null
)

data class NotificationSettingsResponseData(
    @SerializedName("privacyAlertEnabled") val privacyAlertEnabled: Boolean,
    @SerializedName("studyReminderEnabled") val studyReminderEnabled: Boolean,
    @SerializedName("returnRewardEnabled") val returnRewardEnabled: Boolean,
    @SerializedName("questRewardEnabled") val questRewardEnabled: Boolean,
    @SerializedName("marketingEnabled") val marketingEnabled: Boolean
)

