package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** GET /parent/me 의 data 본문 */
data class ParentMeResponseData(
    @SerializedName("parentId") val parentId: String,
    @SerializedName("email") val email: String?,
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean,
    @SerializedName("childrenCount") val childrenCount: Int,
    @SerializedName("createdAt") val createdAt: String
)

