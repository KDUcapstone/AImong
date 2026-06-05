package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

data class ParentRegisterRequest(
    @SerializedName("nickname") val nickname: String
)

data class ParentRegisterResponse(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("code") val code: String,
    /** v2.3: NORMAL 기본 티켓 지급 장수(온보딩·자녀 추가 공통, MVP 기본 3) */
    @SerializedName("starterTickets") val starterTickets: Int,
)

data class ChildLoginRequest(
    @SerializedName("code") val code: String
)

data class ChildLoginResponse(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("sessionToken") val sessionToken: String,
    /**
     * 세션 무효화 버전. v2.3 기준 검증은 JWT payload의 sessionVersion이 정본이며,
     * 응답 body에 없으면 클라이언트는 1로 저장(서버 AuthFilter가 JWT와 DB를 비교).
     */
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
