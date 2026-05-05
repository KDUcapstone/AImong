package com.kduniv.aimong.core.network.model

import com.google.gson.annotations.SerializedName

/** GET /parent/children 의 `data` 본문 */
data class ParentChildrenResponseData(
    @SerializedName("children") val children: List<ParentChildItem> = emptyList()
)

data class ParentChildItem(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("code") val code: String,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int
)
