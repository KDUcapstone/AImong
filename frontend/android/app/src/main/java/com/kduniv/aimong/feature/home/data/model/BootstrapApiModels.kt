package com.kduniv.aimong.feature.home.data.model

import com.google.gson.annotations.SerializedName

/** GET /app/bootstrap 의 `data` */
data class BootstrapResponseData(
    @SerializedName("authenticated") val authenticated: Boolean = false,
    @SerializedName("authType") val authType: String? = null,
    @SerializedName("serverTime") val serverTime: String? = null,
    @SerializedName("serverDate") val serverDate: String? = null,
    @SerializedName("minimumAppVersion") val minimumAppVersion: String? = null,
    @SerializedName("forceUpdateRequired") val forceUpdateRequired: Boolean = false,
    @SerializedName("parent") val parent: BootstrapParentDto? = null,
    @SerializedName("children") val children: List<BootstrapChildBriefDto>? = null,
    @SerializedName("child") val child: BootstrapChildBriefDto? = null,
    @SerializedName("homeAvailable") val homeAvailable: Boolean? = null
)

data class BootstrapParentDto(
    @SerializedName("parentId") val parentId: String? = null,
    @SerializedName("childrenCount") val childrenCount: Int? = null,
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean? = null
)

data class BootstrapChildBriefDto(
    @SerializedName("childId") val childId: String? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageType") val profileImageType: String? = null,
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null,
    @SerializedName("totalXp") val totalXp: Int? = null
)
