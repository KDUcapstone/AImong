package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** GET /parent/children 의 `data` 본문 */
data class ParentChildrenResponseData(
    @SerializedName("children") val children: List<ParentChildItem> = emptyList(),
    /** 일부 BE: 부모 계정 표시명 */
    @SerializedName("parentNickname") val parentNickname: String? = null
)

data class ParentChildItem(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("code") val code: String,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int,
    /** v2.0: 자녀 FCM 토큰 유무(원문 미노출) */
    @SerializedName("hasFcmToken") val hasFcmToken: Boolean? = null,
    /** v2.0: 마지막 활동 시각(미연동 판단에 사용 가능) */
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null,
    /** v2.0: 생성 시각 */
    @SerializedName("createdAt") val createdAt: String? = null
)
