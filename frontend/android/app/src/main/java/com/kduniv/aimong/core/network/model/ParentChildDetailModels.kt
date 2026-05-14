package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** GET /parent/children/{childId} 의 data 본문 */
data class ParentChildDetailResponseData(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("code") val code: String,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int,
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean,
    @SerializedName("lastActiveAt") val lastActiveAt: String?,
    @SerializedName("createdAt") val createdAt: String
)

