package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

data class ParentRegisterRequest(
    @SerializedName("nickname") val nickname: String
)

data class ParentRegisterResponse(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("code") val code: String,
    @SerializedName("starterTickets") val starterTickets: Int
)

data class ChildLoginRequest(
    @SerializedName("code") val code: String
)

data class ChildLoginResponse(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("sessionToken") val sessionToken: String,
    /** API v1.5 세션 버전. 없으면 클라이언트에서 1로 간주. */
    @SerializedName(value = "sessionVersion", alternate = ["session_version"])
    val sessionVersion: Int? = null,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int
)

data class ParentFcmTokenRequest(
    @SerializedName("fcmToken") val fcmToken: String
)

data class ParentFcmTokenResponse(
    @SerializedName("registered") val registered: Boolean
)
